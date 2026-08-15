package net.bitnp.guildofpioneers.common;

import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.function.BooleanSupplier;

/**
 * Centralizes generic user-level permission checks. Module-specific rules
 * (e.g. project leadership) are supplied by the calling module as a
 * {@link BooleanSupplier}; this service only knows about the user's identity
 * and department memberships, so adding a new override such as the admin
 * bypass is done in one place here.
 */
@Service
public class PermissionService {

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;

    public PermissionService(
            UserRepository userRepository,
            UserDepartmentRepository userDepartmentRepository
    ) {
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
    }

    /**
     * Resolves the current user from the authentication. The lookup ignores
     * case, so any casing of the username matches.
     *
     * @param authentication the current authentication
     * @return the resolved user
     * @throws UserNotFoundException if the current user cannot be resolved
     */
    public User currentUser(Authentication authentication) {
        return userRepository.findByUserNameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(authentication.getName()));
    }

    /**
     * Returns whether the user is a member of the given department.
     *
     * @param user       the user to check
     * @param department the department to test membership in
     * @return true if the user belongs to the department
     */
    public boolean isInDepartment(User user, Department department) {
        return userDepartmentRepository.findByUserId(user.getId()).stream()
                .anyMatch(membership -> membership.getDepartment() == department);
    }

    /**
     * Returns whether the user is a member of the ADMIN department.
     *
     * @param user the user to check
     * @return true if the user belongs to the ADMIN department
     */
    public boolean isAdmin(User user) {
        return isInDepartment(user, Department.ADMIN);
    }

    /**
     * Returns whether the user counts as a manager: a member of the ADMIN
     * department, or holding a LEADER, VICE, or ADVISOR role in any department.
     * Managers may create projects and manage their covers.
     *
     * @param user the user to check
     * @return true if the user is a manager
     */
    public boolean isManager(User user) {
        if (isInDepartment(user, Department.ADMIN)) {
            return true;
        }
        return userDepartmentRepository.findByUserId(user.getId()).stream()
                .anyMatch(membership -> membership.getRole() == DepartmentRole.LEADER
                        || membership.getRole() == DepartmentRole.VICE
                        || membership.getRole() == DepartmentRole.ADVISOR);
    }

    /**
     * Returns true when the user is an admin, or when the module-specific
     * requirement passes. The module supplies its own check, so this service
     * stays free of module knowledge while giving admins an override.
     *
     * @param user              the user to check
     * @param moduleRequirement the module-specific permission check
     * @return true if the user is an admin or the requirement passes
     */
    public boolean isAdminOr(User user, BooleanSupplier moduleRequirement) {
        return isAdmin(user) || moduleRequirement.getAsBoolean();
    }
}
