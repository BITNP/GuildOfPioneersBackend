package net.bitnp.guildofpioneers.user;

import net.bitnp.guildofpioneers.storage.FileStorageService;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

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
        when(userDepartmentRepository.findByUserId(7L)).thenReturn(
                List.of(UserDepartment.builder()
                        .userId(7L)
                        .department(Department.TECH)
                        .role(DepartmentRole.MEMBER)
                        .build())
        );

        AuthResponse response = userService.getCurrentUser(authentication);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getUserName()).isEqualTo("Alice");
        assertThat(response.getDepartments()).extracting(UserDepartmentDto::getDepartment)
                .containsExactly(Department.TECH);
        assertThat(response.getDepartments()).extracting(UserDepartmentDto::getRole)
                .containsExactly(DepartmentRole.MEMBER);
    }

    @Test
    void getCurrentUser_returnsEmptyDepartmentsWhenNotAssigned() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userDepartmentRepository.findByUserId(7L)).thenReturn(List.of());

        AuthResponse response = userService.getCurrentUser(authentication);

        assertThat(response.getDepartments()).isEmpty();
    }

    @Test
    void getUser_returnsProfileWithDepartment() {
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .email("alice@example.com")
                .password("encoded-hash")
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userDepartmentRepository.findByUserId(7L)).thenReturn(
                List.of(UserDepartment.builder()
                        .userId(7L)
                        .department(Department.TECH)
                        .role(DepartmentRole.MEMBER)
                        .build())
        );
        when(fileStorageService.avatarUrl(7L)).thenReturn("/uploads/avatars/7?v=1720000000000");

        AuthResponse response = userService.getUser(7L);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getUserName()).isEqualTo("Alice");
        assertThat(response.getPhone()).isEqualTo("13800000000");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getAvatar()).isEqualTo("/uploads/avatars/7?v=1720000000000");
        assertThat(response.getDepartments()).extracting(UserDepartmentDto::getDepartment)
                .containsExactly(Department.TECH);
        assertThat(response.getDepartments()).extracting(UserDepartmentDto::getRole)
                .containsExactly(DepartmentRole.MEMBER);
    }

    @Test
    void getUser_returnsEmptyDepartmentsWhenNotAssigned() {
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userDepartmentRepository.findByUserId(7L)).thenReturn(List.of());

        AuthResponse response = userService.getUser(7L);

        assertThat(response.getDepartments()).isEmpty();
    }

    @Test
    void getUser_throwsWhenUserMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateAvatar_storesFileAndReturnsResponse() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(42L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(fileStorageService.avatarUrl(42L)).thenReturn("/uploads/avatars/42?v=1720000000000");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8}
        );

        AuthResponse response = userService.updateAvatar(authentication, file);

        verify(fileStorageService).storeAvatar(any(), org.mockito.ArgumentMatchers.eq(42L));
        verify(userRepository, org.mockito.Mockito.never()).save(any());
        assertThat(response.getAvatar()).isEqualTo("/uploads/avatars/42?v=1720000000000");
    }

    @Test
    void updateProfile_updatesFieldsAndReturnsResponse() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .email("alice@example.com")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone("13900000000")
                .email("new@example.com")
                .build();

        AuthResponse response = userService.updateProfile(authentication, request);

        assertThat(user.getUserName()).isEqualTo("Alice");
        assertThat(user.getPhone()).isEqualTo("13900000000");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getUserName()).isEqualTo("Alice");
        assertThat(response.getPhone()).isEqualTo("13900000000");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateProfile_allowsUnchangedOwnPhone() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone("13800000000")
                .build();

        userService.updateProfile(authentication, request);

        verify(userRepository, org.mockito.Mockito.never()).existsByPhone("13800000000");
        assertThat(user.getPhone()).isEqualTo("13800000000");
    }

    @Test
    void updateProfile_rejectsPhoneUsedByAnotherUser() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhone("13900000000")).thenReturn(true);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone("13900000000")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(authentication, request))
                .isInstanceOf(PhoneAlreadyExistsException.class);
    }

    @Test
    void updateProfile_clearsBlankEmail() {
        Authentication authentication = new TestingAuthenticationToken("Alice", null);
        User user = User.builder()
                .id(7L)
                .userName("Alice")
                .phone("13800000000")
                .email("alice@example.com")
                .password("encoded-hash")
                .build();
        when(userRepository.findByUserNameIgnoreCase("Alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone("13800000000")
                .email("")
                .build();

        userService.updateProfile(authentication, request);

        assertThat(user.getEmail()).isNull();
    }
}
