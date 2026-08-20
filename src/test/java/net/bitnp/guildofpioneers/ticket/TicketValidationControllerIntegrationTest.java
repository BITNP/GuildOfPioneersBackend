package net.bitnp.guildofpioneers.ticket;

import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the public registration ticket validation endpoint.
 */
@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class TicketValidationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationTicketRepository registrationTicketRepository;

    @Autowired
    private UserRepository userRepository;

    private Long creatorId;

    @BeforeEach
    void setUp() {
        registrationTicketRepository.deleteAll();
        userRepository.deleteAll();
        creatorId = userRepository.save(User.builder()
                .userName("Creator")
                .phone("13800000000")
                .password("unused")
                .build()).getId();
        registrationTicketRepository.save(RegistrationTicket.builder()
                .code("VALIDCODE123")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdBy(creatorId)
                .department(Department.TECH)
                .role(DepartmentRole.MEMBER)
                .build());
        registrationTicketRepository.save(RegistrationTicket.builder()
                .code("EXPIREDCODE1")
                .createdAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(60))
                .createdBy(creatorId)
                .department(Department.MEDIA)
                .role(DepartmentRole.LEADER)
                .build());
    }

    @AfterEach
    void tearDown() {
        registrationTicketRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void validate_returnsValidTicketWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/tickets/VALIDCODE123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.department").value("TECH"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void validate_reportsExpiredTicketWithInvitedDepartmentAndRole() throws Exception {
        mockMvc.perform(get("/api/tickets/EXPIREDCODE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.expired").value(true))
                .andExpect(jsonPath("$.department").value("MEDIA"))
                .andExpect(jsonPath("$.role").value("LEADER"));
    }

    @Test
    void validate_returnsNotFoundForUnknownCode() throws Exception {
        mockMvc.perform(get("/api/tickets/UNKNOWNCODE"))
                .andExpect(status().isNotFound());
    }
}
