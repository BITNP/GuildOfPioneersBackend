package net.bitnp.guildofpioneers.auth;

import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketRepository;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the login endpoint's remember-me behavior.
 */
@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String USERNAME = "Alice";
    private static final String PHONE = "13000000000";
    private static final String PASSWORD = "password123";

    private Long aliceId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDepartmentRepository userDepartmentRepository;

    @Autowired
    private RegistrationTicketRepository registrationTicketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        registrationTicketRepository.deleteAll();
        userRepository.deleteAll();
        aliceId = userRepository.save(User.builder()
                .userName("Alice")
                .phone(PHONE)
                .email("alice@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build()).getId();
    }

    @Test
    void login_withRememberMe_setsPersistentSessionCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "rememberMe": true
                                }
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(header -> header.contains("JSESSIONID=")
                        && header.contains("Max-Age=2592000")
                        && header.contains("HttpOnly"));
    }

    @Test
    void login_withoutRememberMe_doesNotSetPersistentCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .noneMatch(header -> header.contains("Max-Age=2592000"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(USERNAME)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_grantsDepartmentAndRoleFromTicket() throws Exception {
        registrationTicketRepository.save(RegistrationTicket.builder()
                .code("REGCODE1234")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdBy(aliceId)
                .department(Department.TECH)
                .role(DepartmentRole.MEMBER)
                .build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13100000000",
                                  "password": "secret123",
                                  "userName": "Bob",
                                  "ticketCode": "REGCODE1234",
                                  "email": "bob@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("Bob"))
                .andExpect(jsonPath("$.departments[0].department").value("TECH"))
                .andExpect(jsonPath("$.departments[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.isManager").value(false));

        User bob = userRepository.findByUserNameIgnoreCase("Bob").orElseThrow();
        assertThat(userDepartmentRepository.findByUserId(bob.getId()))
                .anyMatch(membership -> membership.getDepartment() == Department.TECH
                        && membership.getRole() == DepartmentRole.MEMBER);
    }

    @Test
    void updateProfile_changesPhoneAndEmailAndMeReturnsThem() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(put("/api/auth/profile")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13900000000",
                                  "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13900000000"))
                .andExpect(jsonPath("$.email").value("new@example.com"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value(USERNAME))
                .andExpect(jsonPath("$.phone").value("13900000000"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }
}
