package net.bitnp.guildofpioneers.config;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.entity.RegistrationTicket;
import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.entity.UserCloak;
import net.bitnp.guildofpioneers.entity.UserDepartment;
import net.bitnp.guildofpioneers.entity.UserStudent;
import net.bitnp.guildofpioneers.repository.RegistrationTicketRepository;
import net.bitnp.guildofpioneers.repository.UserCloakRepository;
import net.bitnp.guildofpioneers.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.repository.UserRepository;
import net.bitnp.guildofpioneers.repository.UserStudentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds fake development data on startup. Active only when {@code app.seed-data=true}
 * (the default for the local dev configuration) and skipped when the database
 * already contains users, so it never overwrites real data.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final int USER_COUNT = 20;
    private static final int TICKET_COUNT = 5;
    private static final String SEED_PASSWORD = "password123";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12;
    private static final int TICKET_VALIDITY_DAYS = 30;
    private static final String PHONE_PREFIX_DIGITS = "35789";

    private static final String[] NAMES = {
            "WangWei", "LiNing", "ZhangMin", "LiuYang", "ChenJie",
            "YangFan", "HuangTing", "ZhaoLei", "WuQiang", "XuNa",
            "SunJing", "ZhuLin", "MaChao", "HuXin", "GuoHao",
            "HePing", "GaoYuan", "LinXiao", "LuoFei", "ZhengTian"
    };

    private static final String[] DEPARTMENTS = {
            "Technology", "Media", "Outreach", "Operations", "Secretariat"
    };

    private final UserRepository userRepository;
    private final UserStudentRepository userStudentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserCloakRepository userCloakRepository;
    private final RegistrationTicketRepository registrationTicketRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public DataSeeder(
            UserRepository userRepository,
            UserStudentRepository userStudentRepository,
            UserDepartmentRepository userDepartmentRepository,
            UserCloakRepository userCloakRepository,
            RegistrationTicketRepository registrationTicketRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userStudentRepository = userStudentRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.userCloakRepository = userCloakRepository;
        this.registrationTicketRepository = registrationTicketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Populates the database with fake users and registration tickets when it is empty.
     *
     * @param args the application arguments (unused)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Data seeding skipped: database already contains users");
            return;
        }
        List<User> users = seedUsers();
        seedRegistrationTickets(users);
        log.info("Seeded {} fake users and {} registration tickets for development",
                users.size(), TICKET_COUNT);
    }

    private List<User> seedUsers() {
        Set<String> phones = new HashSet<>();
        Set<String> userNames = new HashSet<>();
        List<User> users = new ArrayList<>();
        int studentCount = 0;
        int cloakCount = 0;
        for (int i = 0; i < USER_COUNT; i++) {
            String name = generateUniqueUserName(userNames);
            User user = userRepository.save(User.builder()
                    .userName(name)
                    .phone(generateUniquePhone(phones))
                    .email(name.toLowerCase() + (i + 1) + "@example.com")
                    .password(passwordEncoder.encode(SEED_PASSWORD))
                    .build());
            users.add(user);

            if (random.nextBoolean()) {
                userStudentRepository.save(UserStudent.builder()
                        .userId(user.getId())
                        .studentId(generateStudentId())
                        .build());
                studentCount++;
            }
            if (random.nextBoolean()) {
                userDepartmentRepository.save(UserDepartment.builder()
                        .userId(user.getId())
                        .department(DEPARTMENTS[random.nextInt(DEPARTMENTS.length)])
                        .build());
            }
            if (random.nextBoolean()) {
                userCloakRepository.save(UserCloak.builder()
                        .userId(user.getId())
                        .cloakId(generateCloakId())
                        .build());
                cloakCount++;
            }
        }
        log.info("Seeded {} users ({} with student ids, {} with cloaks)",
                users.size(), studentCount, cloakCount);
        return users;
    }

    private void seedRegistrationTickets(List<User> users) {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < TICKET_COUNT; i++) {
            User creator = users.get(random.nextInt(users.size()));
            registrationTicketRepository.save(RegistrationTicket.builder()
                    .code(uniqueCode(codes))
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plus(TICKET_VALIDITY_DAYS, ChronoUnit.DAYS))
                    .createdBy(creator.getId())
                    .build());
        }
    }

    private String generateUniquePhone(Set<String> phones) {
        String phone;
        do {
            phone = "1" + randomChar(PHONE_PREFIX_DIGITS) + randomDigits(9);
        } while (!phones.add(phone));
        return phone;
    }

    private String generateUniqueUserName(Set<String> userNames) {
        String name = NAMES[random.nextInt(NAMES.length)];
        String uniqueName = name;
        int suffix = 1;
        while (!userNames.add(uniqueName)) {
            uniqueName = name + suffix++;
        }
        return uniqueName;
    }

    private String generateStudentId() {
        return String.format("20%02d%05d", random.nextInt(100), random.nextInt(100000));
    }

    private String generateCloakId() {
        return "CLK-" + randomCode(8);
    }

    private String uniqueCode(Set<String> codes) {
        String code;
        do {
            code = randomCode(CODE_LENGTH);
        } while (!codes.add(code));
        return code;
    }

    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(randomChar(CODE_ALPHABET));
        }
        return code.toString();
    }

    private String randomDigits(int length) {
        StringBuilder digits = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }

    private char randomChar(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
