package net.bitnp.guildofpioneers.auth;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Verified-principal fixtures for business tests; protocol validation has separate end-to-end coverage. */
public final class OidcTestUsers {
    public static final String ISSUER = "http://localhost:8082/realms/guild";
    private OidcTestUsers() { }

    public static GuildOidcUser principal(Long userId, String subject) {
        var token = new OidcIdToken("test-id-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("iss", ISSUER, "sub", subject, "preferred_username", "SuggestedName",
                        "email", "suggested@example.com", "email_verified", true));
        return new GuildOidcUser(new DefaultOidcUser(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("OIDC_USER")), token), userId);
    }

    public static OAuth2AuthenticationToken authentication(Long userId, String subject) {
        GuildOidcUser principal = principal(userId, subject);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "keycloak");
    }

    public static MockHttpSession session(Long userId) { return session(userId, "subject-" + userId); }

    public static MockHttpSession session(Long userId, String subject) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication(userId, subject));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
