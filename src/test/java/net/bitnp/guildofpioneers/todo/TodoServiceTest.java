package net.bitnp.guildofpioneers.todo;

import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.storage.FileStorageService;
import net.bitnp.guildofpioneers.todo.entity.TodoProject;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeader;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeaderKey;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectMember;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectMemberKey;
import net.bitnp.guildofpioneers.todo.exception.InvalidProjectRequestException;
import net.bitnp.guildofpioneers.todo.exception.TodoProjectNotFoundException;
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
}
