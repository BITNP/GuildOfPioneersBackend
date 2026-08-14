package net.bitnp.guildofpioneers.config;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketRepository;
import net.bitnp.guildofpioneers.todo.entity.TodoAction;
import net.bitnp.guildofpioneers.todo.entity.TodoActionMember;
import net.bitnp.guildofpioneers.todo.entity.TodoActionMemberKey;
import net.bitnp.guildofpioneers.todo.entity.TodoProject;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeader;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeaderKey;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectMember;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectMemberKey;
import net.bitnp.guildofpioneers.todo.entity.TodoTask;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskLeader;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskLeaderKey;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskMember;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskMemberKey;
import net.bitnp.guildofpioneers.todo.repository.TodoActionMemberRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoActionRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoProjectLeaderRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoProjectMemberRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoProjectRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoTaskLeaderRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoTaskMemberRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoTaskRepository;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserCloak;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.entity.UserStudent;
import net.bitnp.guildofpioneers.user.repository.UserCloakRepository;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import net.bitnp.guildofpioneers.user.repository.UserStudentRepository;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds fake development data on startup. Active only when {@code app.seed-data=true}
 * (the default for the local dev configuration). Users are seeded when the users
 * table is empty, and todo projects are seeded when the todo_projects table is empty,
 * so existing data is never overwritten.
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

    private static final int PROJECT_COUNT = 3;
    private static final int TASKS_PER_PROJECT = 2;
    private static final int ACTIONS_PER_TASK = 2;
    private static final long FINISH_RANGE_DAYS = 30;

    private static final String[] NAMES = {
            "WangWei", "LiNing", "ZhangMin", "LiuYang", "ChenJie",
            "YangFan", "HuangTing", "ZhaoLei", "WuQiang", "XuNa",
            "SunJing", "ZhuLin", "MaChao", "HuXin", "GuoHao",
            "HePing", "GaoYuan", "LinXiao", "LuoFei", "ZhengTian"
    };

    private static final Department[] DEPARTMENTS = {
            Department.CLINIC, Department.TECH, Department.SUPPORT,
            Department.MEDIA, Department.PRESIDIUM
    };

    private static final DepartmentRole[] ROLES = {
            DepartmentRole.MEMBER, DepartmentRole.LEADER
    };

    private static final String[] PROJECT_TITLES = {
            "Autumn Camp", "Tech Week", "Charity Drive", "Music Festival", "Hackathon"
    };

    private static final String[] TASK_TITLES = {
            "Prepare supplies", "Book venue", "Recruit volunteers", "Design posters",
            "Setup equipment", "Arrange transport"
    };

    private static final String[] ACTION_TITLES = {
            "Buy tents", "Print posters", "Reserve auditorium", "Send invitations",
            "Test equipment", "Draft schedule"
    };

    private final UserRepository userRepository;
    private final UserStudentRepository userStudentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserCloakRepository userCloakRepository;
    private final RegistrationTicketRepository registrationTicketRepository;
    private final TodoProjectRepository todoProjectRepository;
    private final TodoTaskRepository todoTaskRepository;
    private final TodoActionRepository todoActionRepository;
    private final TodoProjectLeaderRepository todoProjectLeaderRepository;
    private final TodoProjectMemberRepository todoProjectMemberRepository;
    private final TodoTaskLeaderRepository todoTaskLeaderRepository;
    private final TodoTaskMemberRepository todoTaskMemberRepository;
    private final TodoActionMemberRepository todoActionMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public DataSeeder(
            UserRepository userRepository,
            UserStudentRepository userStudentRepository,
            UserDepartmentRepository userDepartmentRepository,
            UserCloakRepository userCloakRepository,
            RegistrationTicketRepository registrationTicketRepository,
            TodoProjectRepository todoProjectRepository,
            TodoTaskRepository todoTaskRepository,
            TodoActionRepository todoActionRepository,
            TodoProjectLeaderRepository todoProjectLeaderRepository,
            TodoProjectMemberRepository todoProjectMemberRepository,
            TodoTaskLeaderRepository todoTaskLeaderRepository,
            TodoTaskMemberRepository todoTaskMemberRepository,
            TodoActionMemberRepository todoActionMemberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userStudentRepository = userStudentRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.userCloakRepository = userCloakRepository;
        this.registrationTicketRepository = registrationTicketRepository;
        this.todoProjectRepository = todoProjectRepository;
        this.todoTaskRepository = todoTaskRepository;
        this.todoActionRepository = todoActionRepository;
        this.todoProjectLeaderRepository = todoProjectLeaderRepository;
        this.todoProjectMemberRepository = todoProjectMemberRepository;
        this.todoTaskLeaderRepository = todoTaskLeaderRepository;
        this.todoTaskMemberRepository = todoTaskMemberRepository;
        this.todoActionMemberRepository = todoActionMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Populates the database with fake users, registration tickets, and todo data.
     * Users are seeded only when the users table is empty, and todo projects are
     * seeded only when the todo_projects table is empty, so existing data is never
     * overwritten.
     *
     * @param args the application arguments (unused)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            users = seedUsers();
            seedRegistrationTickets(users);
            log.info("Seeded {} fake users and {} registration tickets for development",
                    users.size(), TICKET_COUNT);
        }
        seedTodoData(users);
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
                Department firstDepartment = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
                userDepartmentRepository.save(UserDepartment.builder()
                        .userId(user.getId())
                        .department(firstDepartment)
                        .role(ROLES[random.nextInt(ROLES.length)])
                        .build());
                if (random.nextInt(4) == 0) {
                    Department secondDepartment = firstDepartment;
                    while (secondDepartment == firstDepartment) {
                        secondDepartment = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
                    }
                    userDepartmentRepository.save(UserDepartment.builder()
                            .userId(user.getId())
                            .department(secondDepartment)
                            .role(DepartmentRole.MEMBER)
                            .build());
                }
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

    /**
     * Seeds a three-layer todo hierarchy: projects with leaders and members, each
     * project with tasks, and each task with actions.
     *
     * @param users the users available to lead and join the todo items
     */
    private void seedTodoData(List<User> users) {
        if (todoProjectRepository.count() > 0) {
            log.info("Todo data seeding skipped: database already contains projects");
            return;
        }
        int taskCount = 0;
        int actionCount = 0;
        for (int p = 0; p < PROJECT_COUNT; p++) {
            List<User> leaders = pickDistinct(users, 1 + random.nextInt(2));
            List<User> members = pickDistinct(users, 3 + random.nextInt(2));
            Instant projectCreated = Instant.now().minus(p + 1L, ChronoUnit.DAYS);
            TodoProject project = todoProjectRepository.save(TodoProject.builder()
                    .title(PROJECT_TITLES[p % PROJECT_TITLES.length])
                    .description("Seed project for " + PROJECT_TITLES[p % PROJECT_TITLES.length])
                    .createdDate(projectCreated)
                    .updatedDate(projectCreated)
                    .endDate(finishDate(projectCreated))
                    .build());
            leaders.forEach(user -> todoProjectLeaderRepository.save(TodoProjectLeader.builder()
                    .id(new TodoProjectLeaderKey(project.getId(), user.getId()))
                    .build()));
            members.forEach(user -> todoProjectMemberRepository.save(TodoProjectMember.builder()
                    .id(new TodoProjectMemberKey(project.getId(), user.getId()))
                    .build()));

            for (int t = 0; t < TASKS_PER_PROJECT; t++) {
                List<User> taskLeaders = pickDistinct(users, 1 + random.nextInt(2));
                List<User> taskMembers = pickDistinct(users, 2 + random.nextInt(2));
                Instant taskCreated = projectCreated.plus(t + 1L, ChronoUnit.HOURS);
                TodoTask task = todoTaskRepository.save(TodoTask.builder()
                        .projectId(project.getId())
                        .title(TASK_TITLES[(p * TASKS_PER_PROJECT + t) % TASK_TITLES.length])
                        .description("Seed task for " + project.getTitle())
                        .createdDate(taskCreated)
                        .updatedDate(taskCreated)
                        .endDate(finishDate(taskCreated))
                        .build());
                taskLeaders.forEach(user -> todoTaskLeaderRepository.save(TodoTaskLeader.builder()
                        .id(new TodoTaskLeaderKey(task.getId(), user.getId()))
                        .build()));
                taskMembers.forEach(user -> todoTaskMemberRepository.save(TodoTaskMember.builder()
                        .id(new TodoTaskMemberKey(task.getId(), user.getId()))
                        .build()));
                taskCount++;

                for (int a = 0; a < ACTIONS_PER_TASK; a++) {
                    List<User> actionMembers = pickDistinct(users, 1 + random.nextInt(2));
                    Instant actionCreated = taskCreated.plus(a + 1L, ChronoUnit.HOURS);
                    TodoAction action = todoActionRepository.save(TodoAction.builder()
                            .taskId(task.getId())
                            .title(ACTION_TITLES[(t * ACTIONS_PER_TASK + a) % ACTION_TITLES.length])
                            .description("Seed action for " + task.getTitle())
                            .createdDate(actionCreated)
                            .updatedDate(actionCreated)
                            .endDate(finishDate(actionCreated))
                            .build());
                    actionMembers.forEach(user -> todoActionMemberRepository.save(TodoActionMember.builder()
                            .id(new TodoActionMemberKey(action.getId(), user.getId()))
                            .build()));
                    actionCount++;
                }
            }
        }
        log.info("Seeded {} todo projects, {} tasks, and {} actions",
                PROJECT_COUNT, taskCount, actionCount);
    }

    private List<User> pickDistinct(List<User> users, int count) {
        List<User> copy = new ArrayList<>(users);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size())).stream().toList();
    }

    private Instant finishDate(Instant created) {
        return random.nextBoolean()
                ? created.plus(7L + random.nextInt((int) FINISH_RANGE_DAYS), ChronoUnit.DAYS)
                : null;
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
