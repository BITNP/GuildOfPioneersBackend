package net.bitnp.guildofpioneers.auth;

import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketRepository;
import net.bitnp.guildofpioneers.user.entity.*;
import net.bitnp.guildofpioneers.user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Verifies admission, browser protection and local-session behavior through the real security chain. */
@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserDepartmentRepository departments;
    @Autowired RegistrationTicketRepository tickets;
    @Autowired OidcIdentityRepository identities;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    Long creatorId;

    @BeforeEach void setUp() {
        tickets.deleteAll();
        users.deleteAll();
        creatorId = users.save(User.builder().userName("Alice").phone("13000000000")
                .email("alice@example.com").password("historical-hash").build()).getId();
        tickets.save(RegistrationTicket.builder().code("REGCODE1234").createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600)).createdBy(creatorId)
                .department(Department.TECH).role(DepartmentRole.MEMBER).build());
    }

    private String registration(String name, String phone) {
        return """
                {"userName":"%s","phone":"%s","email":"bob@example.com","ticketCode":"REGCODE1234"}
                """.formatted(name, phone);
    }

    @Test void statusReportsAnonymousAndPendingSeparatelyAndNeverExposesTokens() throws Exception {
        mvc.perform(get("/api/auth/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("anonymous"));
        mvc.perform(get("/api/auth/status").session(OidcTestUsers.session(null, "new-subject")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("onboarding_required"))
                .andExpect(jsonPath("$.suggestions.userName").value("SuggestedName"))
                .andExpect(jsonPath("$.user").isEmpty()).andExpect(jsonPath("$.idToken").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void pendingIdentityCannotReadAnyBusinessDataOrEditProfiles() throws Exception {
        MockHttpSession pending = OidcTestUsers.session(null, "pending");
        for (String path : new String[]{"/api/auth/me", "/api/users", "/api/todo/projects", "/api/users/" + creatorId}) {
            mvc.perform(get(path).session(pending)).andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/auth/profile").session(pending).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"13900000000\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void registerAtomicallyAdmitsIdentityAndRotatesSessionId() throws Exception {
        MockHttpSession pending = OidcTestUsers.session(null, "new-subject");
        String originalId = pending.getId();
        mvc.perform(post("/api/auth/register").session(pending).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registration("Bob", "13100000000")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userName").value("Bob"))
                .andExpect(jsonPath("$.departments[0].department").value("TECH"))
                .andExpect(jsonPath("$.departments[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.avatar").isNotEmpty());
        assertThat(pending.getId()).isNotEqualTo(originalId);
        Long id = identities.findByIssuerAndSubject(OidcTestUsers.ISSUER, "new-subject").orElseThrow().getUserId();
        assertThat(users.findById(id).orElseThrow().getPassword()).isNull();
        mvc.perform(get("/api/auth/me").session(pending)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/api/auth/register").session(pending).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registration("Other", "13200000000"))).andExpect(status().isForbidden());
    }

    @Test void repeatedIdentityCannotCreateAnOrphanAccount() throws Exception {
        identities.saveAndFlush(new OidcIdentity(creatorId, OidcTestUsers.ISSUER, "existing"));
        mvc.perform(post("/api/auth/register").session(OidcTestUsers.session(null, "existing")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(registration("Bob", "13100000000")))
                .andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test void ordinaryIdentityRequiresTicketAndAnonymousCannotRegister() throws Exception {
        String body = "{\"userName\":\"Bob\",\"phone\":\"13100000000\"}";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").session(OidcTestUsers.session(null, "ordinary")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test void onlyConfiguredIdentityCanBootstrapAndCannotBootstrapWhenAdminExists() throws Exception {
        MockHttpSession pending = OidcTestUsers.session(null, "bootstrap-admin-subject");
        mvc.perform(get("/api/auth/status").session(pending)).andExpect(jsonPath("$.bootstrapAdmin").value(true));
        mvc.perform(post("/api/auth/register").session(pending).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"Maintainer\",\"phone\":\"13100000000\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.departments[0].department").value("ADMIN"))
                .andExpect(jsonPath("$.isManager").value(true));
        mvc.perform(get("/api/auth/status").session(OidcTestUsers.session(null, "bootstrap-admin-subject")))
                .andExpect(jsonPath("$.bootstrapAdmin").value(false));
        mvc.perform(post("/api/auth/register").session(OidcTestUsers.session(null, "bootstrap-admin-subject"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"OtherAdmin\",\"phone\":\"13200000000\"}"))
                .andExpect(status().isConflict());
    }

    @Test void realCsrfEndpointProtectsWritesAndLocalLogoutClearsSession() throws Exception {
        MockHttpSession session = OidcTestUsers.session(creatorId);
        mvc.perform(put("/api/auth/profile").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000000\"}")).andExpect(status().isForbidden());
        var result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var token = mapper.readTree(result.getResponse().getContentAsString());
        mvc.perform(put("/api/auth/profile").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"13900000000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("13900000000"));
        mvc.perform(post("/api/auth/logout").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent()).andExpect(header().doesNotExist("Location"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void passwordLoginNoLongerAuthenticates() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Alice\",\"password\":\"historical-hash\"}"))
                .andExpect(status().isUnauthorized());
    }
}
