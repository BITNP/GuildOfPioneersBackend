package net.bitnp.guildofpioneers.controller;

import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the login endpoint's remember-me behavior.
 */
@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String PHONE = "13000000000";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .userName("Alice")
                .phone(PHONE)
                .email("alice@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());
    }

    @Test
    void login_withRememberMe_setsPersistentSessionCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "password": "%s",
                                  "rememberMe": true
                                }
                                """.formatted(PHONE, PASSWORD)))
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
                                  "phone": "%s",
                                  "password": "%s"
                                }
                                """.formatted(PHONE, PASSWORD)))
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
                                  "phone": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(PHONE)))
                .andExpect(status().isUnauthorized());
    }
}
