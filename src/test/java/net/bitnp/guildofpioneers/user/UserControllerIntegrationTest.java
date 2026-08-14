package net.bitnp.guildofpioneers.user;

import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the user profile endpoints, including profile and
 * avatar editing by the user themselves and by admins.
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
    private UserDepartmentRepository userDepartmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private User target;
    private User admin;
    private MockHttpSession session;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .userName(USERNAME)
                .phone("13000000000")
                .email("alice@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());
        target = userRepository.save(User.builder()
                .userName("Bob")
                .phone("13000000001")
                .email("bob@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());
        admin = userRepository.save(User.builder()
                .userName("Admin")
                .phone("13000000002")
                .email("admin@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());
        userDepartmentRepository.save(UserDepartment.builder()
                .userId(admin.getId())
                .department(Department.ADMIN)
                .role(DepartmentRole.LEADER)
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

        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Admin",
                                  "password": "%s"
                                }
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        adminSession = (MockHttpSession) adminLogin.getRequest().getSession();
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

    @Test
    void selfUpdatesOwnProfile_viaUsersEndpoint_returnsUpdatedProfile() throws Exception {
        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13100000000",
                                  "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.userName").value(USERNAME))
                .andExpect(jsonPath("$.phone").value("13100000000"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void adminUpdatesOtherUserProfile_returnsUpdatedProfile() throws Exception {
        mockMvc.perform(put("/api/users/{id}", target.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13900000000",
                                  "email": "bob-new@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId().intValue()))
                .andExpect(jsonPath("$.userName").value("Bob"))
                .andExpect(jsonPath("$.phone").value("13900000000"))
                .andExpect(jsonPath("$.email").value("bob-new@example.com"));
    }

    @Test
    void nonAdminUpdatesOtherUserProfile_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/users/{id}", target.getId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13900000000",
                                  "email": "hijacked@example.com"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonAdminUpdatesOtherUserAvatar_returnsForbidden() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/{id}/avatar", target.getId())
                        .file(avatar)
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_missingUser_returnsNotFound() throws Exception {
        mockMvc.perform(put("/api/users/{id}", 9999L)
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13900000000",
                                  "email": "nobody@example.com"
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}
