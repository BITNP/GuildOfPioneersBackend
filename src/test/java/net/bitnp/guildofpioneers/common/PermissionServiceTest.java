package net.bitnp.guildofpioneers.common;

import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PermissionService}.
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDepartmentRepository userDepartmentRepository;

    private PermissionService permissionService;

    private final User user = User.builder()
            .id(1L)
            .userName("Alice")
            .phone("13800000000")
            .password("encoded-hash")
            .build();

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(userRepository, userDepartmentRepository);
    }

    private void stubDepartments(List<UserDepartment> departments) {
        when(userDepartmentRepository.findByUserId(user.getId())).thenReturn(departments);
    }

    private UserDepartment department(Department department, DepartmentRole role) {
        return UserDepartment.builder()
                .userId(user.getId())
                .department(department)
                .role(role)
                .build();
    }

    @Test
    void isManager_returnsTrueForAdminDepartmentMember() {
        stubDepartments(List.of(department(Department.ADMIN, DepartmentRole.MEMBER)));
        assertThat(permissionService.isManager(user)).isTrue();
    }

    @Test
    void isManager_returnsTrueForLeaderRole() {
        stubDepartments(List.of(department(Department.TECH, DepartmentRole.LEADER)));
        assertThat(permissionService.isManager(user)).isTrue();
    }

    @Test
    void isManager_returnsTrueForViceRole() {
        stubDepartments(List.of(department(Department.TECH, DepartmentRole.VICE)));
        assertThat(permissionService.isManager(user)).isTrue();
    }

    @Test
    void isManager_returnsTrueForAdvisorRole() {
        stubDepartments(List.of(department(Department.TECH, DepartmentRole.ADVISOR)));
        assertThat(permissionService.isManager(user)).isTrue();
    }

    @Test
    void isManager_returnsFalseForMemberRoleOnly() {
        stubDepartments(List.of(department(Department.TECH, DepartmentRole.MEMBER)));
        assertThat(permissionService.isManager(user)).isFalse();
    }

    @Test
    void isManager_returnsFalseForUserWithoutDepartments() {
        stubDepartments(List.of());
        assertThat(permissionService.isManager(user)).isFalse();
    }
}
