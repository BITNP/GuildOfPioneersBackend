package net.bitnp.guildofpioneers.ticket;

import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.PermissionDeniedException;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationTicketService}.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationTicketServiceTest {

    @Mock
    private RegistrationTicketRepository registrationTicketRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RegistrationTicketService registrationTicketService;

    private final User creator = User.builder().id(7L).userName("Alice").phone("13800000000").build();

    private CreateRegistrationTicketRequest request(Department department, DepartmentRole role) {
        return CreateRegistrationTicketRequest.builder()
                .expiresAt(Instant.parse("2026-09-01T00:00:00Z"))
                .department(department)
                .role(role)
                .build();
    }

    @Test
    void create_generatesCodeAndPersistsTicket() {
        when(permissionService.currentUser(authentication)).thenReturn(creator);
        when(permissionService.isAdminOrPresidium(creator)).thenReturn(true);
        when(registrationTicketRepository.existsByCode(anyString())).thenReturn(false);
        when(registrationTicketRepository.save(any(RegistrationTicket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateRegistrationTicketRequest request = request(Department.TECH, DepartmentRole.MEMBER);

        RegistrationTicketResponse response = registrationTicketService.create(request, authentication);

        ArgumentCaptor<RegistrationTicket> captor = ArgumentCaptor.forClass(RegistrationTicket.class);
        verify(registrationTicketRepository).save(captor.capture());
        RegistrationTicket saved = captor.getValue();
        assertThat(saved.getCode()).hasSize(12).matches("[A-Z2-9]+");
        assertThat(saved.getCreatedBy()).isEqualTo(7L);
        assertThat(saved.getExpiresAt()).isEqualTo(request.getExpiresAt());
        assertThat(saved.getDepartment()).isEqualTo(Department.TECH);
        assertThat(saved.getRole()).isEqualTo(DepartmentRole.MEMBER);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(response.getCode()).isEqualTo(saved.getCode());
        assertThat(response.getCreatedBy()).isEqualTo(7L);
        assertThat(response.getDepartment()).isEqualTo(Department.TECH);
        assertThat(response.getRole()).isEqualTo(DepartmentRole.MEMBER);
    }

    @Test
    void create_throwsWhenCreatorNotFound() {
        when(permissionService.currentUser(authentication))
                .thenThrow(new UserNotFoundException("alice"));

        CreateRegistrationTicketRequest request = request(Department.TECH, DepartmentRole.MEMBER);

        assertThatThrownBy(() -> registrationTicketService.create(request, authentication))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_throwsWhenCreatorIsNotAdminOrPresidium() {
        when(permissionService.currentUser(authentication)).thenReturn(creator);
        when(permissionService.isAdminOrPresidium(creator)).thenReturn(false);

        CreateRegistrationTicketRequest request = request(Department.TECH, DepartmentRole.MEMBER);

        assertThatThrownBy(() -> registrationTicketService.create(request, authentication))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void create_throwsWhenDepartmentIsAdmin() {
        when(permissionService.currentUser(authentication)).thenReturn(creator);
        when(permissionService.isAdminOrPresidium(creator)).thenReturn(true);

        CreateRegistrationTicketRequest request = request(Department.ADMIN, DepartmentRole.MEMBER);

        assertThatThrownBy(() -> registrationTicketService.create(request, authentication))
                .isInstanceOf(InvalidTicketRequestException.class);
    }

    @Test
    void validate_passesForValidTicket() {
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code("VALIDCODE123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(registrationTicketRepository.findByCode("VALIDCODE123")).thenReturn(Optional.of(ticket));

        registrationTicketService.validate("VALIDCODE123");
    }

    @Test
    void validate_throwsWhenTicketNotFound() {
        when(registrationTicketRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationTicketService.validate("MISSING"))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void validate_throwsWhenTicketExpired() {
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code("EXPIRED123")
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(registrationTicketRepository.findByCode("EXPIRED123")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> registrationTicketService.validate("EXPIRED123"))
                .isInstanceOf(TicketExpiredException.class);
    }

    @Test
    void validateTicket_reportsValidTicket() {
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code("VALIDCODE123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .department(Department.TECH)
                .role(DepartmentRole.LEADER)
                .build();
        when(registrationTicketRepository.findByCode("VALIDCODE123")).thenReturn(Optional.of(ticket));

        TicketValidationResponse response = registrationTicketService.validateTicket("VALIDCODE123");

        assertThat(response.isValid()).isTrue();
        assertThat(response.isExpired()).isFalse();
        assertThat(response.getDepartment()).isEqualTo(Department.TECH);
        assertThat(response.getRole()).isEqualTo(DepartmentRole.LEADER);
        assertThat(response.getExpiresAt()).isEqualTo(ticket.getExpiresAt());
    }

    @Test
    void validateTicket_reportsExpiredTicketWithInvitedDepartmentAndRole() {
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code("EXPIRED123")
                .expiresAt(Instant.now().minusSeconds(60))
                .department(Department.MEDIA)
                .role(DepartmentRole.MEMBER)
                .build();
        when(registrationTicketRepository.findByCode("EXPIRED123")).thenReturn(Optional.of(ticket));

        TicketValidationResponse response = registrationTicketService.validateTicket("EXPIRED123");

        assertThat(response.isValid()).isFalse();
        assertThat(response.isExpired()).isTrue();
        assertThat(response.getDepartment()).isEqualTo(Department.MEDIA);
        assertThat(response.getRole()).isEqualTo(DepartmentRole.MEMBER);
    }

    @Test
    void validateTicket_throwsWhenTicketNotFound() {
        when(registrationTicketRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationTicketService.validateTicket("MISSING"))
                .isInstanceOf(TicketCodeNotFoundException.class);
    }
}
