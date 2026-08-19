package net.bitnp.guildofpioneers.config;

import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminSeeder}.
 */
@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments args;

    private AdminSeeder adminSeeder;

    @BeforeEach
    void setUp() {
        adminSeeder = new AdminSeeder(userRepository, userDepartmentRepository,
                passwordEncoder, "secret123");
    }

    @Test
    void run_whenAdminMissing_createsAdminWithAdminDepartment() {
        when(userRepository.findByUserNameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        adminSeeder.run(args);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUserName()).isEqualTo("admin");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-hash");

        ArgumentCaptor<UserDepartment> departmentCaptor = ArgumentCaptor.forClass(UserDepartment.class);
        verify(userDepartmentRepository).save(departmentCaptor.capture());
        assertThat(departmentCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(departmentCaptor.getValue().getDepartment()).isEqualTo(Department.ADMIN);
        assertThat(departmentCaptor.getValue().getRole()).isEqualTo(DepartmentRole.LEADER);
    }

    @Test
    void run_whenAdminExists_doesNotCreateAnother() {
        User existing = User.builder().id(1L).userName("admin").build();
        when(userRepository.findByUserNameIgnoreCase("admin")).thenReturn(Optional.of(existing));

        adminSeeder.run(args);

        verify(userRepository, never()).save(any(User.class));
        verify(userDepartmentRepository, never()).save(any(UserDepartment.class));
    }
}
