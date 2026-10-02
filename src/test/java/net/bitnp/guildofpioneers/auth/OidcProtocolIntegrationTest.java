package net.bitnp.guildofpioneers.auth;

import java.net.URI;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises real discovery, signed ID token validation, nonce, PKCE and browser-bound state. */
@SpringBootTest(properties = {"app.seed-data=false",
        "spring.datasource.url=jdbc:h2:mem:protocol;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=KEY"})
@AutoConfigureMockMvc
class OidcProtocolIntegrationTest {
    static final MockOidcProvider provider = new MockOidcProvider();
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired OidcIdentityRepository identities;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.oidc.issuer", provider::issuer);
    }
    @AfterAll static void stop() { provider.close(); }

    @BeforeEach void clear() { users.deleteAll(); }

    record Login(MockHttpSession session, String state, String nonce, String challenge) { }

    private Login begin(String returnTo) throws Exception {
        var initial = mvc.perform(get("/api/auth/login").param("returnTo", returnTo))
                .andExpect(status().isFound()).andReturn();
        var session = (MockHttpSession) initial.getRequest().getSession();
        var authorization = mvc.perform(get("/api/auth/oidc/authorize/keycloak").session(session))
                .andExpect(status().isFound()).andReturn();
        URI location = URI.create(authorization.getResponse().getRedirectedUrl());
        var query = MockOidcProvider.parse(location.getRawQuery());
        assertThat(query.get("redirect_uri")).isEqualTo("http://localhost:5173/api/auth/oidc/callback/keycloak");
        assertThat(query.get("code_challenge_method")).isEqualTo("S256");
        return new Login(session, query.get("state"), query.get("nonce"), query.get("code_challenge"));
    }

    @Test void unboundIdentityWithSameProfileAsOldUserDoesNotAutoMerge() throws Exception {
        users.save(User.builder().userName("Alice").phone("13800000000").email("alice@example.com").build());
        Login login = begin("/projects/42?view=tree");
        provider.grant("valid", login.nonce(), login.challenge(), "valid");
        mvc.perform(get("/api/auth/oidc/callback/keycloak").session(login.session())
                        .param("code", "valid").param("state", login.state()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/register?redirect=%2Fprojects%2F42%3Fview%3Dtree"));
        assertThat(provider.pkceVerified).isTrue();
        mvc.perform(get("/api/auth/status").session(login.session()))
                .andExpect(jsonPath("$.state").value("onboarding_required"));
        mvc.perform(get("/api/users").session(login.session())).andExpect(status().isForbidden());
        assertThat(identities.count()).isZero();
    }

    @Test void explicitlyBoundIdentityReturnsToRequestedPageWithoutOverwritingProfile() throws Exception {
        Long id = users.save(User.builder().userName("LocalName").phone("13800000000").email("local@example.com").build()).getId();
        identities.saveAndFlush(new OidcIdentity(id, provider.issuer(), "protocol-subject"));
        Login login = begin("/projects/42");
        String originalId = login.session().getId();
        provider.grant("bound", login.nonce(), login.challenge(), "valid");
        mvc.perform(get("/api/auth/oidc/callback/keycloak").session(login.session())
                        .param("code", "bound").param("state", login.state()))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/projects/42"));
        assertThat(login.session().getId()).isNotEqualTo(originalId);
        mvc.perform(get("/api/auth/me").session(login.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.userName").value("LocalName"))
                .andExpect(jsonPath("$.email").value("local@example.com"))
                .andExpect(jsonPath("$.isManager").value(false));
    }

    @ParameterizedTest @ValueSource(strings = {"nonce", "issuer", "audience", "expired", "signature"})
    void invalidIdTokenNeverEstablishesAuthentication(String mode) throws Exception {
        Login login = begin("/");
        provider.grant(mode, login.nonce(), login.challenge(), mode);
        mvc.perform(get("/api/auth/oidc/callback/keycloak").session(login.session())
                        .param("code", mode).param("state", login.state()))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/login?error=oidc_failed"));
        mvc.perform(get("/api/auth/status").session(login.session())).andExpect(jsonPath("$.state").value("anonymous"));
        mvc.perform(get("/api/auth/me").session(login.session())).andExpect(status().isUnauthorized());
    }

    @Test void stateCannotBeUsedFromAnotherBrowserOrReplayed() throws Exception {
        Login login = begin("/");
        int before = provider.exchanges.get();
        mvc.perform(get("/api/auth/oidc/callback/keycloak").session(new MockHttpSession())
                        .param("code", "unused").param("state", login.state()))
                .andExpect(redirectedUrl("/login?error=oidc_failed"));
        assertThat(provider.exchanges.get()).isEqualTo(before);
        provider.grant("valid-once", login.nonce(), login.challenge(), "valid");
        mvc.perform(get("/api/auth/oidc/callback/keycloak").session(login.session())
                        .param("code", "valid-once").param("state", login.state()))
                .andExpect(status().isFound());
        int exchanged = provider.exchanges.get();
        mvc.perform(get("/api/auth/oidc/callback/keycloak").session(login.session())
                        .param("code", "valid-once").param("state", login.state()))
                .andExpect(redirectedUrl("/login?error=oidc_failed"));
        assertThat(provider.exchanges.get()).isEqualTo(exchanged);
    }

    @Test void failedDiscoveryIsRetryableAndRedirectsStayOnSite() {
        var properties = new OidcProperties(provider.issuer(), "guild-test", "test-secret", "http://localhost:5173", "");
        var repository = new LazyOidcClientRegistrationRepository(properties);
        provider.metadataUnavailable = true;
        try {
            assertThatThrownBy(() -> repository.findByRegistrationId("keycloak"))
                    .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
        } finally { provider.metadataUnavailable = false; }
        assertThat(repository.findByRegistrationId("keycloak")).isNotNull();
        for (String path : new String[]{"https://evil.example", "//evil.example", "/\\evil.example", "/%2f%2fevil.example", "/login", "/api/auth/login"}) {
            assertThat(LoginRedirects.safePath(path)).isEqualTo("/");
        }
        assertThat(LoginRedirects.safePath("/projects/42?view=tree")).isEqualTo("/projects/42?view=tree");
    }
}
