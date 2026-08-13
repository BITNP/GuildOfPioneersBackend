package net.bitnp.guildofpioneers.todo;

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
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the read-only todo endpoints.
 */
@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class TodoControllerIntegrationTest {

    private static final String USERNAME = "Alice";
    private static final String PHONE = "13000000000";
    private static final String PASSWORD = "password123";
    private static final Instant CREATED = Instant.parse("2026-08-13T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TodoProjectRepository projectRepository;

    @Autowired
    private TodoTaskRepository taskRepository;

    @Autowired
    private TodoActionRepository actionRepository;

    @Autowired
    private TodoProjectLeaderRepository projectLeaderRepository;

    @Autowired
    private TodoProjectMemberRepository projectMemberRepository;

    @Autowired
    private TodoTaskLeaderRepository taskLeaderRepository;

    @Autowired
    private TodoTaskMemberRepository taskMemberRepository;

    @Autowired
    private TodoActionMemberRepository actionMemberRepository;

    private User leader;
    private User member;
    private TodoProject project;
    private TodoTask task;
    private TodoAction action;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        actionMemberRepository.deleteAll();
        actionRepository.deleteAll();
        taskMemberRepository.deleteAll();
        taskLeaderRepository.deleteAll();
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectLeaderRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        leader = userRepository.save(User.builder()
                .userName(USERNAME)
                .phone(PHONE)
                .email("alice@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());
        member = userRepository.save(User.builder()
                .userName("Bob")
                .phone("13000000001")
                .email("bob@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .build());

        project = projectRepository.save(TodoProject.builder()
                .title("Autumn Camp")
                .description("Annual autumn camp")
                .createdDate(CREATED)
                .updatedDate(CREATED)
                .build());
        projectLeaderRepository.save(TodoProjectLeader.builder()
                .id(new TodoProjectLeaderKey(project.getId(), leader.getId()))
                .build());
        projectMemberRepository.save(TodoProjectMember.builder()
                .id(new TodoProjectMemberKey(project.getId(), member.getId()))
                .build());

        task = taskRepository.save(TodoTask.builder()
                .projectId(project.getId())
                .title("Prepare supplies")
                .description("Buy camping supplies")
                .createdDate(CREATED)
                .updatedDate(CREATED)
                .build());
        taskLeaderRepository.save(TodoTaskLeader.builder()
                .id(new TodoTaskLeaderKey(task.getId(), leader.getId()))
                .build());
        taskMemberRepository.save(TodoTaskMember.builder()
                .id(new TodoTaskMemberKey(task.getId(), member.getId()))
                .build());

        action = actionRepository.save(TodoAction.builder()
                .taskId(task.getId())
                .title("Write report")
                .description("Draft the final report")
                .createdDate(CREATED)
                .updatedDate(CREATED)
                .build());
        actionMemberRepository.save(TodoActionMember.builder()
                .id(new TodoActionMemberKey(action.getId(), member.getId()))
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void listProjects_returnsProjectWithLeadersMembersAndNullCover() throws Exception {
        mockMvc.perform(get("/api/todo/projects").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(project.getId().intValue()))
                .andExpect(jsonPath("$[0].title").value("Autumn Camp"))
                .andExpect(jsonPath("$[0].description").value("Annual autumn camp"))
                .andExpect(jsonPath("$[0].cover").value(nullValue()))
                .andExpect(jsonPath("$[0].createdDate").exists())
                .andExpect(jsonPath("$[0].updatedDate").exists())
                .andExpect(jsonPath("$[0].endDate").value(nullValue()))
                .andExpect(jsonPath("$[0].leaderIds[0]").value(leader.getId().intValue()))
                .andExpect(jsonPath("$[0].memberIds[0]").value(member.getId().intValue()));
    }

    @Test
    void listProjects_sortsByUpdatedDateDesc() throws Exception {
        TodoProject newer = projectRepository.save(TodoProject.builder()
                .title("Newer Project")
                .description("Recently updated")
                .createdDate(CREATED)
                .updatedDate(CREATED.plusSeconds(3600))
                .build());

        mockMvc.perform(get("/api/todo/projects").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(newer.getId().intValue()))
                .andExpect(jsonPath("$[0].title").value("Newer Project"))
                .andExpect(jsonPath("$[1].id").value(project.getId().intValue()));
    }

    @Test
    void getProject_returnsProject() throws Exception {
        mockMvc.perform(get("/api/todo/projects/{projectId}", project.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().intValue()))
                .andExpect(jsonPath("$.title").value("Autumn Camp"))
                .andExpect(jsonPath("$.leaderIds[0]").value(leader.getId().intValue()))
                .andExpect(jsonPath("$.memberIds[0]").value(member.getId().intValue()));
    }

    @Test
    void listTasks_returnsTasksOfProject() throws Exception {
        mockMvc.perform(get("/api/todo/tasks").param("projectId", project.getId().toString()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(task.getId().intValue()))
                .andExpect(jsonPath("$[0].projectId").value(project.getId().intValue()))
                .andExpect(jsonPath("$[0].title").value("Prepare supplies"))
                .andExpect(jsonPath("$[0].leaderIds[0]").value(leader.getId().intValue()))
                .andExpect(jsonPath("$[0].memberIds[0]").value(member.getId().intValue()));
    }

    @Test
    void getTask_returnsTask() throws Exception {
        mockMvc.perform(get("/api/todo/tasks/{taskId}", task.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId().intValue()))
                .andExpect(jsonPath("$.projectId").value(project.getId().intValue()))
                .andExpect(jsonPath("$.title").value("Prepare supplies"));
    }

    @Test
    void listActions_returnsActionsWithoutLeaders() throws Exception {
        mockMvc.perform(get("/api/todo/actions").param("taskId", task.getId().toString()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(action.getId().intValue()))
                .andExpect(jsonPath("$[0].taskId").value(task.getId().intValue()))
                .andExpect(jsonPath("$[0].title").value("Write report"))
                .andExpect(jsonPath("$[0].leaderIds").doesNotExist())
                .andExpect(jsonPath("$[0].memberIds[0]").value(member.getId().intValue()));
    }

    @Test
    void getAction_returnsAction() throws Exception {
        mockMvc.perform(get("/api/todo/actions/{actionId}", action.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(action.getId().intValue()))
                .andExpect(jsonPath("$.taskId").value(task.getId().intValue()))
                .andExpect(jsonPath("$.title").value("Write report"));
    }

    @Test
    void missingResources_returnNotFound() throws Exception {
        mockMvc.perform(get("/api/todo/projects/{projectId}", 9999L).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/todo/tasks").param("projectId", "9999").session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/todo/tasks/{taskId}", 9999L).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/todo/actions").param("taskId", "9999").session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/todo/actions/{actionId}", 9999L).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(get("/api/todo/projects"))
                .andExpect(status().isUnauthorized());
    }
}
