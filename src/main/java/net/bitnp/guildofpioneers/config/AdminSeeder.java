package net.bitnp.guildofpioneers.config;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the dedicated site admin account exists on startup.
 *
 * <p>Unlike the development-only {@link DataSeeder}, this runner is active in all
 * environments (including production, where {@code app.seed-data} is disabled) so a
 * maintainer can always log in. It creates the admin only when the username is
 * absent, so an existing account is never overwritten.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.admin-seed", havingValue = "true", matchIfMissing = true)
public class AdminSeeder implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PHONE = "13800000001";
    private static final String ADMIN_EMAIL = "admin@example.com";

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            UserDepartmentRepository userDepartmentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-password:password123}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    /**
     * Creates the admin account with the ADMIN department when it does not already
     * exist.
     *
     * @param args the application arguments (unused)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByUserNameIgnoreCase(ADMIN_USERNAME).isPresent()) {
            return;
        }
        User admin = userRepository.save(User.builder()
                .userName(ADMIN_USERNAME)
                .phone(ADMIN_PHONE)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(adminPassword))
                .build());
        userDepartmentRepository.save(UserDepartment.builder()
                .userId(admin.getId())
                .department(Department.ADMIN)
                .role(DepartmentRole.LEADER)
                .build());
        log.info("Seeded admin user {}", ADMIN_USERNAME);
    }
}
