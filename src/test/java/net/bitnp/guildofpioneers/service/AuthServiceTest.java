package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.dto.request.RegisterRequest;
import net.bitnp.guildofpioneers.dto.response.AuthResponse;
import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.exception.TicketExpiredException;
import net.bitnp.guildofpioneers.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegistrationTicketService registrationTicketService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_encodesPasswordAndSavesUser() {
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        RegisterRequest request = RegisterRequest.builder()
                .phone("13800000000")
                .password("secret123")
                .userName("Alice")
                .avatar("avatar-url")
                .ticketCode("VALIDCODE123")
                .email("alice@example.com")
                .build();

        AuthResponse response = authService.register(request);

        verify(registrationTicketService).validate("VALIDCODE123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-hash");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPhone()).isEqualTo("13800000000");
    }

    @Test
    void register_rejectsDuplicatePhone() {
        when(userRepository.existsByPhone("13800000000")).thenReturn(true);

        RegisterRequest request = RegisterRequest.builder()
                .phone("13800000000")
                .password("secret123")
                .userName("Alice")
                .avatar("avatar-url")
                .ticketCode("VALIDCODE123")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(PhoneAlreadyExistsException.class);
    }

    @Test
    void register_rejectsExpiredTicket() {
        doThrow(new TicketExpiredException("EXPIRED123")).when(registrationTicketService)
                .validate("EXPIRED123");

        RegisterRequest request = RegisterRequest.builder()
                .phone("13800000000")
                .password("secret123")
                .userName("Alice")
                .avatar("avatar-url")
                .ticketCode("EXPIRED123")
                .build();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(TicketExpiredException.class);
    }
}
