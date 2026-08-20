package net.bitnp.guildofpioneers.auth;

import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketService;
import net.bitnp.guildofpioneers.ticket.TicketExpiredException;
import net.bitnp.guildofpioneers.user.AuthResponse;
import net.bitnp.guildofpioneers.user.UserDepartmentDto;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegistrationTicketService registrationTicketService;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AuthService authService;

    private RegistrationTicket ticket(String code) {
        return RegistrationTicket.builder()
                .code(code)
                .expiresAt(Instant.now().plusSeconds(3600))
                .department(Department.TECH)
                .role(DepartmentRole.MEMBER)
                .build();
    }

    @Test
    void register_encodesPasswordAndSavesUser() {
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(registrationTicketService.findByCode("VALIDCODE123")).thenReturn(ticket("VALIDCODE123"));
        when(userDepartmentRepository.save(any(UserDepartment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userDepartmentRepository.findByUserId(1L)).thenReturn(List.of(
                UserDepartment.builder()
                        .id(10L)
                        .userId(1L)
                        .department(Department.TECH)
                        .role(DepartmentRole.MEMBER)
                        .build()
        ));
        when(permissionService.isManager(any(User.class))).thenReturn(false);

        RegisterRequest request = RegisterRequest.builder()
                .phone("13800000000")
                .password("secret123")
                .userName("Alice")
                .ticketCode("VALIDCODE123")
                .email("alice@example.com")
                .build();

        AuthResponse response = authService.register(request);

        verify(registrationTicketService).validate("VALIDCODE123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-hash");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");

        ArgumentCaptor<UserDepartment> membershipCaptor = ArgumentCaptor.forClass(UserDepartment.class);
        verify(userDepartmentRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(membershipCaptor.getValue().getDepartment()).isEqualTo(Department.TECH);
        assertThat(membershipCaptor.getValue().getRole()).isEqualTo(DepartmentRole.MEMBER);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPhone()).isEqualTo("13800000000");
        assertThat(response.getDepartments())
                .extracting(UserDepartmentDto::getDepartment)
                .containsExactly(Department.TECH);
        assertThat(response.getDepartments())
                .extracting(UserDepartmentDto::getRole)
                .containsExactly(DepartmentRole.MEMBER);
        assertThat(response.getIsManager()).isFalse();
    }

    @Test
    void register_rejectsDuplicatePhone() {
        when(userRepository.existsByPhone("13800000000")).thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .phone("13800000000")
                .password("secret123")
                .userName("Alice")
                .ticketCode("VALIDCODE123")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(PhoneAlreadyExistsException.class);
    }

    @Test
    void register_rejectsDuplicateUserName() {
        when(userRepository.existsByUserNameIgnoreCase("Alice")).thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .phone("13900000000")
                .password("secret123")
                .userName("Alice")
                .ticketCode("VALIDCODE123")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserNameAlreadyExistsException.class);
    }

    @Test
    void register_rejectsExpiredTicket() {
        doThrow(new TicketExpiredException("EXPIRED123")).when(registrationTicketService)
                .validate("EXPIRED123");

        RegisterRequest request = RegisterRequest.builder()
                .phone("13800000000")
                .password("secret123")
                .userName("Alice")
                .ticketCode("EXPIRED123")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(TicketExpiredException.class);
    }
}
