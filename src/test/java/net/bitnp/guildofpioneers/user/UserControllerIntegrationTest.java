package net.bitnp.guildofpioneers.user;

import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the public user profile endpoint.
 */
@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    private static final String USERNAME = "Alice";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .userName(USERNAME)
                .phone("13000000000")
                .email("alice@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());

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
        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void getUser_returnsFullProfile() throws Exception {
        mockMvc.perform(get("/api/users/{id}", user.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.userName").value(USERNAME))
                .andExpect(jsonPath("$.phone").value("13000000000"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.avatar", startsWith("/uploads/avatars/")));
    }

    @Test
    void getUser_returnsNotFoundForMissingUser() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 9999L).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }
}
