package net.bitnp.guildofpioneers.todo;

import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.storage.FileStorageService;
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
import net.bitnp.guildofpioneers.todo.exception.InvalidActionRequestException;
import net.bitnp.guildofpioneers.todo.exception.InvalidProjectRequestException;
import net.bitnp.guildofpioneers.todo.exception.InvalidTaskRequestException;
import net.bitnp.guildofpioneers.todo.exception.TodoActionNotFoundException;
import net.bitnp.guildofpioneers.todo.exception.TodoProjectNotFoundException;
import net.bitnp.guildofpioneers.todo.exception.TodoTaskNotFoundException;
import net.bitnp.guildofpioneers.todo.repository.TodoActionMemberRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoActionRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoProjectLeaderRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoProjectMemberRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoProjectRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoTaskLeaderRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoTaskMemberRepository;
import net.bitnp.guildofpioneers.todo.repository.TodoTaskRepository;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.PermissionDeniedException;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TodoService} project creation and cover management.
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoProjectRepository todoProjectRepository;
    @Mock
    private TodoTaskRepository todoTaskRepository;
    @Mock
    private TodoActionRepository todoActionRepository;
    @Mock
    private TodoProjectLeaderRepository todoProjectLeaderRepository;
    @Mock
    private TodoProjectMemberRepository todoProjectMemberRepository;
    @Mock
    private TodoTaskLeaderRepository todoTaskLeaderRepository;
    @Mock
    private TodoTaskMemberRepository todoTaskMemberRepository;
    @Mock
    private TodoActionMemberRepository todoActionMemberRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PermissionService permissionService;

    private TodoService todoService;

    private final Authentication authentication = new TestingAuthenticationToken("Alice", null);

    private final User user = User.builder()
            .id(1L)
            .userName("Alice")
            .phone("13800000000")
            .password("encoded-hash")
            .build();

    @BeforeEach
    void setUp() {
        todoService = new TodoService(
                todoProjectRepository, todoTaskRepository, todoActionRepository,
                todoProjectLeaderRepository, todoProjectMemberRepository,
                todoTaskLeaderRepository, todoTaskMemberRepository,
                todoActionMemberRepository, fileStorageService,
                userRepository, permissionService);
    }

    @Test
    void createProject_byManager_createsProjectWithLeadersAndMembers() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isManager(user)).thenReturn(true);
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user, user, user));
        TodoProject saved = TodoProject.builder()
                .id(10L)
                .title("Hackathon")
                .description("Annual hackathon")
                .createdDate(Instant.parse("2026-08-15T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-15T08:00:00Z"))
                .build();
        when(todoProjectRepository.save(any())).thenReturn(saved);
        when(todoProjectLeaderRepository.findById_ProjectId(10L)).thenReturn(List.of(
                TodoProjectLeader.builder().id(new TodoProjectLeaderKey(10L, 1L)).build(),
                TodoProjectLeader.builder().id(new TodoProjectLeaderKey(10L, 2L)).build()));
        when(todoProjectMemberRepository.findById_ProjectId(10L)).thenReturn(List.of(
                TodoProjectMember.builder().id(new TodoProjectMemberKey(10L, 3L)).build()));

        CreateProjectRequest request = CreateProjectRequest.builder()
                .title("Hackathon")
                .description("Annual hackathon")
                .leaderIds(List.of(1L, 2L))
                .memberIds(List.of(3L))
                .build();

        TodoProjectUpdateResponse response = todoService.createProject(request, authentication);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Hackathon");
        assertThat(response.getLeaderIds()).containsExactly(1L, 2L);
        assertThat(response.getMemberIds()).containsExactly(3L);
        verify(todoProjectLeaderRepository).save(argThat(leader -> leader.getId().getProjectId() == 10L
                && leader.getId().getUserId() == 1L));
        verify(todoProjectLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 2L));
        verify(todoProjectMemberRepository).save(argThat(member -> member.getId().getUserId() == 3L));
    }

    @Test
    void createProject_addsCreatorAsDefaultLeader() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isManager(user)).thenReturn(true);
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user, user));
        TodoProject saved = TodoProject.builder()
                .id(12L)
                .title("Hackathon")
                .createdDate(Instant.parse("2026-08-15T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-15T08:00:00Z"))
                .build();
        when(todoProjectRepository.save(any())).thenReturn(saved);
        when(todoProjectLeaderRepository.findById_ProjectId(12L)).thenReturn(List.of(
                TodoProjectLeader.builder().id(new TodoProjectLeaderKey(12L, 2L)).build(),
                TodoProjectLeader.builder().id(new TodoProjectLeaderKey(12L, 1L)).build()));

        CreateProjectRequest request = CreateProjectRequest.builder()
                .title("Hackathon")
                .leaderIds(List.of(2L))
                .memberIds(List.of())
                .build();

        TodoProjectUpdateResponse response = todoService.createProject(request, authentication);

        assertThat(response.getLeaderIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(todoProjectLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 1L));
        verify(todoProjectLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 2L));
        verify(todoProjectMemberRepository, never()).save(any());
    }

    @Test
    void createProject_deduplicatesAndDropsNullIds() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isManager(user)).thenReturn(true);
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user));
        TodoProject saved = TodoProject.builder()
                .id(11L)
                .title("Hackathon")
                .createdDate(Instant.parse("2026-08-15T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-15T08:00:00Z"))
                .build();
        when(todoProjectRepository.save(any())).thenReturn(saved);

        CreateProjectRequest request = CreateProjectRequest.builder()
                .title("Hackathon")
                .leaderIds(Arrays.asList(1L, 1L, null))
                .memberIds(List.of())
                .build();

        todoService.createProject(request, authentication);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).findAllById(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L);
        verify(todoProjectLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 1L));
    }

    @Test
    void createProject_byNonManager_throwsPermissionDenied() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isManager(user)).thenReturn(false);

        CreateProjectRequest request = CreateProjectRequest.builder()
                .title("Hackathon")
                .build();

        assertThatThrownBy(() -> todoService.createProject(request, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoProjectRepository, never()).save(any());
    }

    @Test
    void createProject_overlappingLeaderAndMember_throwsInvalidRequest() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isManager(user)).thenReturn(true);

        CreateProjectRequest request = CreateProjectRequest.builder()
                .title("Hackathon")
                .leaderIds(List.of(1L, 2L))
                .memberIds(List.of(2L))
                .build();

        assertThatThrownBy(() -> todoService.createProject(request, authentication))
                .isInstanceOf(InvalidProjectRequestException.class);
        verify(todoProjectRepository, never()).save(any());
    }

    @Test
    void createProject_unknownUser_throwsInvalidRequest() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isManager(user)).thenReturn(true);
        when(userRepository.findAllById(anyList())).thenReturn(List.of());

        CreateProjectRequest request = CreateProjectRequest.builder()
                .title("Hackathon")
                .leaderIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.createProject(request, authentication))
                .isInstanceOf(InvalidProjectRequestException.class);
        verify(todoProjectRepository, never()).save(any());
    }

    @Test
    void updateProject_replacesLeadersAndMembers() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user, user));
        List<TodoProjectLeader> oldLeaders = List.of(
                TodoProjectLeader.builder().id(new TodoProjectLeaderKey(1L, 1L)).build());
        List<TodoProjectLeader> newLeaders = List.of(
                TodoProjectLeader.builder().id(new TodoProjectLeaderKey(1L, 2L)).build());
        List<TodoProjectMember> oldMembers = List.of(
                TodoProjectMember.builder().id(new TodoProjectMemberKey(1L, 2L)).build());
        List<TodoProjectMember> newMembers = List.of(
                TodoProjectMember.builder().id(new TodoProjectMemberKey(1L, 3L)).build());
        when(todoProjectLeaderRepository.findById_ProjectId(1L)).thenReturn(oldLeaders, newLeaders);
        when(todoProjectMemberRepository.findById_ProjectId(1L)).thenReturn(oldMembers, newMembers);
        when(todoProjectRepository.save(any())).thenReturn(project);

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .title("Autumn Camp 2026")
                .description("Annual autumn camp")
                .leaderIds(List.of(2L))
                .memberIds(List.of(3L))
                .build();

        TodoProjectUpdateResponse response = todoService.updateProject(1L, request, authentication);

        assertThat(response.getLeaderIds()).containsExactly(2L);
        assertThat(response.getMemberIds()).containsExactly(3L);
        verify(todoProjectLeaderRepository).deleteAll(oldLeaders);
        verify(todoProjectMemberRepository).deleteAll(oldMembers);
        verify(todoProjectLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 2L));
        verify(todoProjectMemberRepository).save(argThat(member -> member.getId().getUserId() == 3L));
        verify(todoProjectRepository).save(project);
    }

    @Test
    void updateProject_absentLists_leaveMembershipUnchanged() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectRepository.save(any())).thenReturn(project);

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .title("Autumn Camp 2026")
                .description("Annual autumn camp")
                .build();

        todoService.updateProject(1L, request, authentication);

        verify(todoProjectLeaderRepository, never()).deleteAll(any());
        verify(todoProjectMemberRepository, never()).deleteAll(any());
        verify(todoProjectLeaderRepository, never()).save(any());
        verify(todoProjectMemberRepository, never()).save(any());
        verify(todoProjectRepository).save(project);
    }

    @Test
    void updateProject_overlappingLeaderAndMember_throwsInvalidRequest() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .title("Autumn Camp 2026")
                .leaderIds(List.of(1L, 2L))
                .memberIds(List.of(2L))
                .build();

        assertThatThrownBy(() -> todoService.updateProject(1L, request, authentication))
                .isInstanceOf(InvalidProjectRequestException.class);
        verify(todoProjectRepository, never()).save(any());
    }

    @Test
    void updateProject_unknownUser_throwsInvalidRequest() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(userRepository.findAllById(anyList())).thenReturn(List.of());

        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .title("Autumn Camp 2026")
                .leaderIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.updateProject(1L, request, authentication))
                .isInstanceOf(InvalidProjectRequestException.class);
        verify(todoProjectRepository, never()).save(any());
    }

    @Test
    void uploadProjectCover_byLeader_storesCoverAndBumpsUpdatedDate() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(fileStorageService.storeProjectCover(any(), any())).thenReturn("/uploads/project_covers/1?v=1720000000000");
        when(fileStorageService.projectCoverUrl(1L)).thenReturn("/uploads/project_covers/1?v=1720000000000");

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[]{1}
        );

        TodoProjectUpdateResponse response = todoService.uploadProjectCover(1L, file, authentication);

        assertThat(response.getCover()).isEqualTo("/uploads/project_covers/1?v=1720000000000");
        verify(fileStorageService).storeProjectCover(file, 1L);
        verify(todoProjectRepository).save(project);
        assertThat(project.getUpdatedDate()).isAfter(Instant.parse("2026-08-13T08:00:00Z"));
    }

    @Test
    void uploadProjectCover_byNonLeader_throwsPermissionDenied() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[]{1}
        );

        assertThatThrownBy(() -> todoService.uploadProjectCover(1L, file, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(fileStorageService, never()).storeProjectCover(any(), any());
    }

    @Test
    void uploadProjectCover_missingProject_throwsNotFound() {
        when(todoProjectRepository.findById(999L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[]{1}
        );

        assertThatThrownBy(() -> todoService.uploadProjectCover(999L, file, authentication))
                .isInstanceOf(TodoProjectNotFoundException.class);
        verify(fileStorageService, never()).storeProjectCover(any(), any());
    }

    @Test
    void createTask_byProjectMember_createsTaskWithCreatorAsLeader() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectLeaderRepository.existsById(any())).thenReturn(true);
        TodoTask saved = TodoTask.builder()
                .id(10L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-15T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-15T08:00:00Z"))
                .build();
        when(todoTaskRepository.save(any())).thenReturn(saved);
        when(todoProjectRepository.save(any())).thenReturn(project);
        when(todoTaskLeaderRepository.findById_TaskId(10L)).thenReturn(List.of(
                TodoTaskLeader.builder().id(new TodoTaskLeaderKey(10L, 1L)).build()));
        when(todoTaskMemberRepository.findById_TaskId(10L)).thenReturn(List.of(
                TodoTaskMember.builder().id(new TodoTaskMemberKey(10L, 2L)).build()));

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Prepare supplies")
                .leaderIds(List.of(1L))
                .memberIds(List.of(2L))
                .build();

        TodoTaskUpdateResponse response = todoService.createTask(request, authentication);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Prepare supplies");
        assertThat(response.getLeaderIds()).containsExactly(1L);
        assertThat(response.getMemberIds()).containsExactly(2L);
        verify(todoTaskLeaderRepository).save(argThat(leader -> leader.getId().getTaskId() == 10L
                && leader.getId().getUserId() == 1L));
        verify(todoTaskMemberRepository).save(argThat(member -> member.getId().getUserId() == 2L));
        verify(todoProjectRepository).save(project);
    }

    @Test
    void createTask_addsCreatorAsDefaultLeader() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectLeaderRepository.existsById(any())).thenReturn(true);
        TodoTask saved = TodoTask.builder()
                .id(12L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-15T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-15T08:00:00Z"))
                .build();
        when(todoTaskRepository.save(any())).thenReturn(saved);
        when(todoProjectRepository.save(any())).thenReturn(project);
        when(todoTaskLeaderRepository.findById_TaskId(12L)).thenReturn(List.of(
                TodoTaskLeader.builder().id(new TodoTaskLeaderKey(12L, 2L)).build(),
                TodoTaskLeader.builder().id(new TodoTaskLeaderKey(12L, 1L)).build()));

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Prepare supplies")
                .leaderIds(List.of(2L))
                .memberIds(List.of())
                .build();

        TodoTaskUpdateResponse response = todoService.createTask(request, authentication);

        assertThat(response.getLeaderIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(todoTaskLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 1L));
        verify(todoTaskLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 2L));
        verify(todoTaskMemberRepository, never()).save(any());
    }

    @Test
    void createTask_byNonProjectMember_throwsPermissionDenied() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Hijacked")
                .build();

        assertThatThrownBy(() -> todoService.createTask(request, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void createTask_overlappingLeaderAndMember_throwsInvalidTaskRequest() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Prepare supplies")
                .leaderIds(List.of(1L, 2L))
                .memberIds(List.of(2L))
                .build();

        assertThatThrownBy(() -> todoService.createTask(request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void createTask_unknownUser_throwsInvalidTaskRequest() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Prepare supplies")
                .leaderIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.createTask(request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void createTask_withUserNotInProject_throwsInvalidTaskRequest() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Prepare supplies")
                .leaderIds(List.of(1L))
                .memberIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.createTask(request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void createTask_byAdmin_withUserNotInProject_throwsInvalidTaskRequest() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(1L)
                .title("Prepare supplies")
                .leaderIds(List.of(99L))
                .memberIds(List.of())
                .build();

        assertThatThrownBy(() -> todoService.createTask(request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void createTask_missingProject_throwsNotFound() {
        when(todoProjectRepository.findById(999L)).thenReturn(Optional.empty());

        CreateTaskRequest request = CreateTaskRequest.builder()
                .projectId(999L)
                .title("Prepare supplies")
                .build();

        assertThatThrownBy(() -> todoService.createTask(request, authentication))
                .isInstanceOf(TodoProjectNotFoundException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void updateTask_replacesLeadersAndMembers() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        TodoTask task = TodoTask.builder()
                .id(1L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectLeaderRepository.existsById(any())).thenReturn(true);
        List<TodoTaskLeader> oldLeaders = List.of(
                TodoTaskLeader.builder().id(new TodoTaskLeaderKey(1L, 1L)).build());
        List<TodoTaskLeader> newLeaders = List.of(
                TodoTaskLeader.builder().id(new TodoTaskLeaderKey(1L, 2L)).build());
        List<TodoTaskMember> oldMembers = List.of(
                TodoTaskMember.builder().id(new TodoTaskMemberKey(1L, 2L)).build());
        List<TodoTaskMember> newMembers = List.of(
                TodoTaskMember.builder().id(new TodoTaskMemberKey(1L, 3L)).build());
        when(todoTaskLeaderRepository.findById_TaskId(1L)).thenReturn(oldLeaders, newLeaders);
        when(todoTaskMemberRepository.findById_TaskId(1L)).thenReturn(oldMembers, newMembers);
        when(todoTaskRepository.save(any())).thenReturn(task);
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(todoProjectRepository.save(any())).thenReturn(project);

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Prepare supplies for camp")
                .leaderIds(List.of(2L))
                .memberIds(List.of(3L))
                .build();

        TodoTaskUpdateResponse response = todoService.updateTask(1L, request, authentication);

        assertThat(response.getLeaderIds()).containsExactly(2L);
        assertThat(response.getMemberIds()).containsExactly(3L);
        verify(todoTaskLeaderRepository).deleteAll(oldLeaders);
        verify(todoTaskMemberRepository).deleteAll(oldMembers);
        verify(todoTaskLeaderRepository).save(argThat(leader -> leader.getId().getUserId() == 2L));
        verify(todoTaskMemberRepository).save(argThat(member -> member.getId().getUserId() == 3L));
        verify(todoTaskRepository).save(task);
    }

    @Test
    void updateTask_absentLists_leaveMembershipUnchanged() {
        TodoProject project = TodoProject.builder()
                .id(1L)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        TodoTask task = TodoTask.builder()
                .id(1L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoTaskRepository.save(any())).thenReturn(task);
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(todoProjectRepository.save(any())).thenReturn(project);
        when(todoTaskLeaderRepository.findById_TaskId(1L)).thenReturn(List.of());
        when(todoTaskMemberRepository.findById_TaskId(1L)).thenReturn(List.of());

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Prepare supplies for camp")
                .build();

        todoService.updateTask(1L, request, authentication);

        verify(todoTaskLeaderRepository, never()).deleteAll(any());
        verify(todoTaskMemberRepository, never()).deleteAll(any());
        verify(todoTaskLeaderRepository, never()).save(any());
        verify(todoTaskMemberRepository, never()).save(any());
        verify(todoTaskRepository).save(task);
    }

    @Test
    void updateTask_overlappingLeaderAndMember_throwsInvalidTaskRequest() {
        TodoTask task = TodoTask.builder()
                .id(1L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Prepare supplies")
                .leaderIds(List.of(1L, 2L))
                .memberIds(List.of(2L))
                .build();

        assertThatThrownBy(() -> todoService.updateTask(1L, request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void updateTask_unknownUser_throwsInvalidTaskRequest() {
        TodoTask task = TodoTask.builder()
                .id(1L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Prepare supplies")
                .leaderIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.updateTask(1L, request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void updateTask_withUserNotInProject_throwsInvalidTaskRequest() {
        TodoTask task = TodoTask.builder()
                .id(1L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Prepare supplies")
                .leaderIds(List.of(1L))
                .memberIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.updateTask(1L, request, authentication))
                .isInstanceOf(InvalidTaskRequestException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void updateTask_byNonTaskLeader_throwsPermissionDenied() {
        TodoTask task = TodoTask.builder()
                .id(1L)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Hijacked")
                .build();

        assertThatThrownBy(() -> todoService.updateTask(1L, request, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    @Test
    void updateTask_missingTask_throwsNotFound() {
        when(todoTaskRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Prepare supplies")
                .build();

        assertThatThrownBy(() -> todoService.updateTask(999L, request, authentication))
                .isInstanceOf(TodoTaskNotFoundException.class);
        verify(todoTaskRepository, never()).save(any());
    }

    private TodoProject todoProject(Long id) {
        return TodoProject.builder()
                .id(id)
                .title("Autumn Camp")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
    }

    private TodoTask todoTask(Long id) {
        return TodoTask.builder()
                .id(id)
                .projectId(1L)
                .title("Prepare supplies")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
    }

    private TodoAction todoAction(Long id) {
        return TodoAction.builder()
                .id(id)
                .taskId(1L)
                .title("Write report")
                .createdDate(Instant.parse("2026-08-13T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-13T08:00:00Z"))
                .build();
    }

    private void stubTouchHierarchy(TodoTask task) {
        when(todoProjectRepository.findById(1L)).thenReturn(Optional.of(todoProject(1L)));
        when(todoProjectRepository.save(any())).thenReturn(todoProject(1L));
        when(todoTaskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(todoTaskRepository.save(any())).thenReturn(task);
    }

    @Test
    void createAction_byTaskMember_createsActionWithCreatorAsMember() {
        TodoTask task = todoTask(1L);
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectMemberRepository.existsById(any())).thenReturn(true);
        TodoAction saved = TodoAction.builder()
                .id(20L)
                .taskId(1L)
                .title("Draft outline")
                .description("Outline the report")
                .createdDate(Instant.parse("2026-08-15T08:00:00Z"))
                .updatedDate(Instant.parse("2026-08-15T08:00:00Z"))
                .build();
        when(todoActionRepository.save(any())).thenReturn(saved);
        stubTouchHierarchy(task);
        when(todoActionMemberRepository.findById_ActionId(20L)).thenReturn(List.of(
                TodoActionMember.builder().id(new TodoActionMemberKey(20L, 2L)).build(),
                TodoActionMember.builder().id(new TodoActionMemberKey(20L, 1L)).build()));

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(1L)
                .title("Draft outline")
                .description("Outline the report")
                .memberIds(List.of(2L))
                .build();

        TodoActionResponse response = todoService.createAction(request, authentication);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getTaskId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Draft outline");
        assertThat(response.getDescription()).isEqualTo("Outline the report");
        assertThat(response.getMemberIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(todoActionMemberRepository).save(argThat(m -> m.getId().getUserId() == 1L));
        verify(todoActionMemberRepository).save(argThat(m -> m.getId().getUserId() == 2L));
        verify(todoTaskRepository).save(task);
    }

    @Test
    void createAction_byNonTaskMember_throwsPermissionDenied() {
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(todoTask(1L)));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(1L)
                .title("Hijacked")
                .build();

        assertThatThrownBy(() -> todoService.createAction(request, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void createAction_withUserNotInProject_throwsInvalidActionRequest() {
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(todoTask(1L)));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(1L)
                .title("Prepare supplies")
                .memberIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.createAction(request, authentication))
                .isInstanceOf(InvalidActionRequestException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void createAction_byAdmin_canAssignProjectMemberNotInTask() {
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectMemberRepository.existsById(any())).thenReturn(true);
        TodoAction saved = todoAction(20L);
        when(todoActionRepository.save(any())).thenReturn(saved);
        stubTouchHierarchy(todoTask(1L));
        when(todoActionMemberRepository.findById_ActionId(20L)).thenReturn(List.of(
                TodoActionMember.builder().id(new TodoActionMemberKey(20L, 99L)).build(),
                TodoActionMember.builder().id(new TodoActionMemberKey(20L, 1L)).build()));

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(1L)
                .title("Book venue")
                .memberIds(List.of(99L))
                .build();

        TodoActionResponse response = todoService.createAction(request, authentication);

        assertThat(response.getMemberIds()).containsExactlyInAnyOrder(1L, 99L);
        verify(todoActionMemberRepository).save(argThat(m -> m.getId().getUserId() == 99L));
    }

    @Test
    void createAction_byAdmin_withUserNotInProject_throwsInvalidActionRequest() {
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(todoTask(1L)));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(1L)
                .title("Book venue")
                .memberIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.createAction(request, authentication))
                .isInstanceOf(InvalidActionRequestException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void createAction_addsProjectMemberNotInTaskToTask() {
        TodoTask task = todoTask(1L);
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectMemberRepository.existsById(any())).thenReturn(true);
        TodoAction saved = todoAction(20L);
        when(todoActionRepository.save(any())).thenReturn(saved);
        stubTouchHierarchy(task);
        when(todoTaskLeaderRepository.findById_TaskId(1L)).thenReturn(List.of(
                TodoTaskLeader.builder().id(new TodoTaskLeaderKey(1L, 1L)).build()));
        when(todoTaskMemberRepository.findById_TaskId(1L)).thenReturn(List.of());
        when(todoActionMemberRepository.findById_ActionId(20L)).thenReturn(List.of(
                TodoActionMember.builder().id(new TodoActionMemberKey(20L, 1L)).build(),
                TodoActionMember.builder().id(new TodoActionMemberKey(20L, 2L)).build()));

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(1L)
                .title("Draft outline")
                .memberIds(List.of(2L))
                .build();

        todoService.createAction(request, authentication);

        verify(todoTaskMemberRepository).save(argThat(m -> m.getId().getTaskId() == 1L
                && m.getId().getUserId() == 2L));
        verify(todoTaskMemberRepository, never()).save(argThat(m -> m.getId().getUserId() == 1L));
    }

    @Test
    void createAction_missingTask_throwsNotFound() {
        when(todoTaskRepository.findById(999L)).thenReturn(Optional.empty());

        CreateActionRequest request = CreateActionRequest.builder()
                .taskId(999L)
                .title("Prepare supplies")
                .build();

        assertThatThrownBy(() -> todoService.createAction(request, authentication))
                .isInstanceOf(TodoTaskNotFoundException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void updateAction_byActionMember_replacesMembers() {
        TodoAction action = todoAction(1L);
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoProjectMemberRepository.existsById(any())).thenReturn(true);
        List<TodoActionMember> oldMembers = List.of(
                TodoActionMember.builder().id(new TodoActionMemberKey(1L, 1L)).build());
        List<TodoActionMember> newMembers = List.of(
                TodoActionMember.builder().id(new TodoActionMemberKey(1L, 2L)).build());
        when(todoActionMemberRepository.findById_ActionId(1L)).thenReturn(oldMembers, newMembers);
        when(todoActionRepository.save(any())).thenReturn(action);
        stubTouchHierarchy(todoTask(1L));

        UpdateActionRequest request = UpdateActionRequest.builder()
                .title("Write the final report")
                .memberIds(List.of(2L))
                .build();

        TodoActionResponse response = todoService.updateAction(1L, request, authentication);

        assertThat(response.getTitle()).isEqualTo("Write the final report");
        assertThat(response.getMemberIds()).containsExactly(2L);
        verify(todoActionMemberRepository).deleteAll(oldMembers);
        verify(todoActionMemberRepository).save(argThat(m -> m.getId().getUserId() == 2L));
        verify(todoActionRepository).save(action);
    }

    @Test
    void updateAction_withUserNotInProject_throwsInvalidActionRequest() {
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(todoAction(1L)));
        when(todoTaskRepository.findById(1L)).thenReturn(Optional.of(todoTask(1L)));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);

        UpdateActionRequest request = UpdateActionRequest.builder()
                .title("Write report")
                .memberIds(List.of(99L))
                .build();

        assertThatThrownBy(() -> todoService.updateAction(1L, request, authentication))
                .isInstanceOf(InvalidActionRequestException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void updateAction_byNonActionMember_throwsPermissionDenied() {
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(todoAction(1L)));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        UpdateActionRequest request = UpdateActionRequest.builder()
                .title("Hijacked")
                .build();

        assertThatThrownBy(() -> todoService.updateAction(1L, request, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void updateAction_missingAction_throwsNotFound() {
        when(todoActionRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateActionRequest request = UpdateActionRequest.builder()
                .title("Write report")
                .build();

        assertThatThrownBy(() -> todoService.updateAction(999L, request, authentication))
                .isInstanceOf(TodoActionNotFoundException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void finishAction_setsEndDateAndPropagates() {
        TodoAction action = todoAction(1L);
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoActionRepository.save(any())).thenReturn(action);
        stubTouchHierarchy(todoTask(1L));
        when(todoActionMemberRepository.findById_ActionId(1L)).thenReturn(List.of());

        TodoActionResponse response = todoService.finishAction(1L, authentication);

        assertThat(response.getEndDate()).isNotNull();
        verify(todoActionRepository).save(action);
    }

    @Test
    void finishAction_byNonActionMember_throwsPermissionDenied() {
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(todoAction(1L)));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        assertThatThrownBy(() -> todoService.finishAction(1L, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoActionRepository, never()).save(any());
    }

    @Test
    void unfinishAction_clearsEndDate() {
        TodoAction action = todoAction(1L);
        action.setEndDate(Instant.parse("2026-08-14T08:00:00Z"));
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(true);
        when(todoActionRepository.save(any())).thenReturn(action);
        stubTouchHierarchy(todoTask(1L));
        when(todoActionMemberRepository.findById_ActionId(1L)).thenReturn(List.of());

        TodoActionResponse response = todoService.unfinishAction(1L, authentication);

        assertThat(response.getEndDate()).isNull();
        verify(todoActionRepository).save(action);
    }

    @Test
    void unfinishAction_byNonActionMember_throwsPermissionDenied() {
        TodoAction action = todoAction(1L);
        action.setEndDate(Instant.parse("2026-08-14T08:00:00Z"));
        when(todoActionRepository.findById(1L)).thenReturn(Optional.of(action));
        when(permissionService.currentUser(authentication)).thenReturn(user);
        when(permissionService.isAdminOr(eq(user), any())).thenReturn(false);

        assertThatThrownBy(() -> todoService.unfinishAction(1L, authentication))
                .isInstanceOf(PermissionDeniedException.class);
        verify(todoActionRepository, never()).save(any());
    }
}
