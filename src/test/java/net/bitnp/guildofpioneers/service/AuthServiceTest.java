package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.dto.request.RegisterRequest;
import net.bitnp.guildofpioneers.dto.response.AuthResponse;
import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.entity.UserDepartment;
import net.bitnp.guildofpioneers.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.exception.TicketExpiredException;
import net.bitnp.guildofpioneers.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

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
    private FileStorageService fileStorageService;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

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

    @Test
    void getCurrentUser_returnsProfileWithDepartment() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userDepartmentRepository.findById(7L)).thenReturn(
                Optional.of(UserDepartment.builder().userId(7L).department("Technology").build())
        );

        AuthResponse response = authService.getCurrentUser(authentication);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getUserName()).isEqualTo("Alice");
        assertThat(response.getDepartment()).isEqualTo("Technology");
    }

    @Test
    void getCurrentUser_returnsNullDepartmentWhenNotAssigned() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userDepartmentRepository.findById(7L)).thenReturn(Optional.empty());

        AuthResponse response = authService.getCurrentUser(authentication);

        assertThat(response.getDepartment()).isNull();
    }

    @Test
    void updateAvatar_storesFileDeletesOldAndSavesUser() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(42L)
                .userName("Alice")
                .avatar("/uploads/avatars/42.png")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(fileStorageService.storeAvatar(any(), org.mockito.ArgumentMatchers.eq(42L)))
                .thenReturn("/uploads/avatars/42.jpg");
        when(fileStorageService.getVersion("/uploads/avatars/42.jpg")).thenReturn(1720000000000L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8}
        );

        AuthResponse response = authService.updateAvatar(authentication, file);

        verify(fileStorageService).deleteAvatar("/uploads/avatars/42.png");
        verify(userRepository).save(user);
        assertThat(user.getAvatar()).isEqualTo("/uploads/avatars/42.jpg");
        assertThat(response.getAvatar()).isEqualTo("/uploads/avatars/42.jpg?v=1720000000000");
    }
}
