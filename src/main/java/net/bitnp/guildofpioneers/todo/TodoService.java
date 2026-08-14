package net.bitnp.guildofpioneers.todo;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.storage.FileStorageService;
import net.bitnp.guildofpioneers.todo.entity.TodoAction;
import net.bitnp.guildofpioneers.todo.entity.TodoProject;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeaderKey;
import net.bitnp.guildofpioneers.todo.entity.TodoTask;
import net.bitnp.guildofpioneers.todo.exception.NotProjectLeaderException;
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
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides access to the three-layer todo hierarchy: projects, tasks, and actions.
 * Read methods expose the hierarchy, while the {@code touch*} methods centralize
 * the "updated time" invariant that propagates activity up the hierarchy. Project
 * title and description editing is available to project leaders; admins override
 * that rule via {@link PermissionService}.
 */
@Slf4j
@Service
public class TodoService {

    private final TodoProjectRepository todoProjectRepository;
    private final TodoTaskRepository todoTaskRepository;
    private final TodoActionRepository todoActionRepository;
    private final TodoProjectLeaderRepository todoProjectLeaderRepository;
    private final TodoProjectMemberRepository todoProjectMemberRepository;
    private final TodoTaskLeaderRepository todoTaskLeaderRepository;
    private final TodoTaskMemberRepository todoTaskMemberRepository;
    private final TodoActionMemberRepository todoActionMemberRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public TodoService(
            TodoProjectRepository todoProjectRepository,
            TodoTaskRepository todoTaskRepository,
            TodoActionRepository todoActionRepository,
            TodoProjectLeaderRepository todoProjectLeaderRepository,
            TodoProjectMemberRepository todoProjectMemberRepository,
            TodoTaskLeaderRepository todoTaskLeaderRepository,
            TodoTaskMemberRepository todoTaskMemberRepository,
            TodoActionMemberRepository todoActionMemberRepository,
            FileStorageService fileStorageService,
            UserRepository userRepository,
            PermissionService permissionService
    ) {
        this.todoProjectRepository = todoProjectRepository;
        this.todoTaskRepository = todoTaskRepository;
        this.todoActionRepository = todoActionRepository;
        this.todoProjectLeaderRepository = todoProjectLeaderRepository;
        this.todoProjectMemberRepository = todoProjectMemberRepository;
        this.todoTaskLeaderRepository = todoTaskLeaderRepository;
        this.todoTaskMemberRepository = todoTaskMemberRepository;
        this.todoActionMemberRepository = todoActionMemberRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    /**
     * Returns all projects ordered by updated date, newest first.
     *
     * @return the project responses
     */
    @Transactional(readOnly = true)
    public List<TodoProjectResponse> listProjects() {
        return todoProjectRepository.findAllByOrderByUpdatedDateDesc().stream()
                .map(this::toProjectResponse)
                .toList();
    }

    /**
     * Returns a single project with its leaders and members.
     *
     * @param projectId the project id
     * @return the project response
     * @throws TodoProjectNotFoundException if the project does not exist
     */
    @Transactional(readOnly = true)
    public TodoProjectResponse getProject(Long projectId) {
        return toProjectResponse(findProject(projectId));
    }

    /**
     * Returns the tasks of a project ordered by updated date, newest first.
     *
     * @param projectId the owning project's id
     * @return the task responses
     * @throws TodoProjectNotFoundException if the project does not exist
     */
    @Transactional(readOnly = true)
    public List<TodoTaskResponse> listTasks(Long projectId) {
        findProject(projectId);
        return todoTaskRepository.findByProjectIdOrderByUpdatedDateDesc(projectId).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    /**
     * Returns a single task with its leaders and members.
     *
     * @param taskId the task id
     * @return the task response
     * @throws TodoTaskNotFoundException if the task does not exist
     */
    @Transactional(readOnly = true)
    public TodoTaskResponse getTask(Long taskId) {
        return toTaskResponse(findTask(taskId));
    }

    /**
     * Returns the actions of a task ordered by updated date, newest first.
     *
     * @param taskId the owning task's id
     * @return the action responses
     * @throws TodoTaskNotFoundException if the task does not exist
     */
    @Transactional(readOnly = true)
    public List<TodoActionResponse> listActions(Long taskId) {
        findTask(taskId);
        return todoActionRepository.findByTaskIdOrderByUpdatedDateDesc(taskId).stream()
                .map(this::toActionResponse)
                .toList();
    }

    /**
     * Returns a single action with its members.
     *
     * @param actionId the action id
     * @return the action response
     * @throws TodoActionNotFoundException if the action does not exist
     */
    @Transactional(readOnly = true)
    public TodoActionResponse getAction(Long actionId) {
        return toActionResponse(findAction(actionId));
    }

    /**
     * Marks a project as updated by setting its updated date to now.
     *
     * <p>This is the top of the hierarchy, so no ancestor needs to be touched.</p>
     *
     * @param projectId the project id
     * @throws TodoProjectNotFoundException if the project does not exist
     */
    @Transactional
    public void touchProject(Long projectId) {
        TodoProject project = findProject(projectId);
        project.setUpdatedDate(Instant.now());
        todoProjectRepository.save(project);
    }

    /**
     * Updates a project's title and description. Only a leader of the project
     * may edit it, unless the current user is an admin, in which case any
     * project may be edited; the change bumps the project's updated date.
     *
     * @param projectId      the project id
     * @param request        the validated title and description
     * @param authentication the current authentication
     * @return the updated project, with member ids but without resolved user summaries
     * @throws TodoProjectNotFoundException if the project does not exist
     * @throws NotProjectLeaderException    if the current user is not a project leader
     */
    @Transactional
    public TodoProjectUpdateResponse updateProject(
            Long projectId, UpdateProjectRequest request, Authentication authentication
    ) {
        TodoProject project = findProject(projectId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isProjectLeader(projectId, user))) {
            throw new NotProjectLeaderException(projectId);
        }
        project.setTitle(request.getTitle());
        project.setDescription(blankToNull(request.getDescription()));
        project.setUpdatedDate(Instant.now());
        log.trace("Project {} updated", projectId);
        return toProjectUpdateResponse(todoProjectRepository.save(project));
    }

    private boolean isProjectLeader(Long projectId, User user) {
        return todoProjectLeaderRepository.existsById(new TodoProjectLeaderKey(projectId, user.getId()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Marks a task as updated and propagates the touch to its owning project.
     *
     * @param taskId the task id
     * @throws TodoTaskNotFoundException if the task does not exist
     */
    @Transactional
    public void touchTask(Long taskId) {
        TodoTask task = findTask(taskId);
        task.setUpdatedDate(Instant.now());
        todoTaskRepository.save(task);
        touchProject(task.getProjectId());
    }

    /**
     * Marks an action as updated and propagates the touch to its owning task,
     * which in turn propagates to the owning project.
     *
     * @param actionId the action id
     * @throws TodoActionNotFoundException if the action does not exist
     */
    @Transactional
    public void touchAction(Long actionId) {
        TodoAction action = findAction(actionId);
        action.setUpdatedDate(Instant.now());
        todoActionRepository.save(action);
        touchTask(action.getTaskId());
    }

    private TodoProject findProject(Long projectId) {
        return todoProjectRepository.findById(projectId)
                .orElseThrow(() -> new TodoProjectNotFoundException(projectId));
    }

    private TodoTask findTask(Long taskId) {
        return todoTaskRepository.findById(taskId)
                .orElseThrow(() -> new TodoTaskNotFoundException(taskId));
    }

    private TodoAction findAction(Long actionId) {
        return todoActionRepository.findById(actionId)
                .orElseThrow(() -> new TodoActionNotFoundException(actionId));
    }

    private TodoProjectResponse toProjectResponse(TodoProject project) {
        List<Long> leaderIds = projectLeaderIds(project.getId());
        List<Long> memberIds = projectMemberIds(project.getId());
        return TodoProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .cover(fileStorageService.projectCoverUrl(project.getId()))
                .description(project.getDescription())
                .createdDate(project.getCreatedDate())
                .updatedDate(project.getUpdatedDate())
                .endDate(project.getEndDate())
                .leaderIds(leaderIds)
                .memberIds(memberIds)
                .leaders(userSummaries(leaderIds))
                .members(userSummaries(memberIds))
                .build();
    }

    private TodoProjectUpdateResponse toProjectUpdateResponse(TodoProject project) {
        return TodoProjectUpdateResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .cover(fileStorageService.projectCoverUrl(project.getId()))
                .description(project.getDescription())
                .createdDate(project.getCreatedDate())
                .updatedDate(project.getUpdatedDate())
                .endDate(project.getEndDate())
                .leaderIds(projectLeaderIds(project.getId()))
                .memberIds(projectMemberIds(project.getId()))
                .build();
    }

    private TodoTaskResponse toTaskResponse(TodoTask task) {
        List<Long> leaderIds = taskLeaderIds(task.getId());
        List<Long> memberIds = taskMemberIds(task.getId());
        return TodoTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .createdDate(task.getCreatedDate())
                .updatedDate(task.getUpdatedDate())
                .endDate(task.getEndDate())
                .leaderIds(leaderIds)
                .memberIds(memberIds)
                .leaders(userSummaries(leaderIds))
                .members(userSummaries(memberIds))
                .build();
    }

    private TodoActionResponse toActionResponse(TodoAction action) {
        return TodoActionResponse.builder()
                .id(action.getId())
                .taskId(action.getTaskId())
                .title(action.getTitle())
                .description(action.getDescription())
                .createdDate(action.getCreatedDate())
                .updatedDate(action.getUpdatedDate())
                .endDate(action.getEndDate())
                .memberIds(actionMemberIds(action.getId()))
                .build();
    }

    private List<Long> projectLeaderIds(Long projectId) {
        return todoProjectLeaderRepository.findById_ProjectId(projectId).stream()
                .map(leader -> leader.getId().getUserId())
                .toList();
    }

    private List<Long> projectMemberIds(Long projectId) {
        return todoProjectMemberRepository.findById_ProjectId(projectId).stream()
                .map(member -> member.getId().getUserId())
                .toList();
    }

    private List<Long> taskLeaderIds(Long taskId) {
        return todoTaskLeaderRepository.findById_TaskId(taskId).stream()
                .map(leader -> leader.getId().getUserId())
                .toList();
    }

    private List<Long> taskMemberIds(Long taskId) {
        return todoTaskMemberRepository.findById_TaskId(taskId).stream()
                .map(member -> member.getId().getUserId())
                .toList();
    }

    private List<Long> actionMemberIds(Long actionId) {
        return todoActionMemberRepository.findById_ActionId(actionId).stream()
                .map(member -> member.getId().getUserId())
                .toList();
    }

    /**
     * Loads the users behind the given ids in one query and maps them to summaries,
     * preserving the order of {@code userIds} and skipping users that no longer exist.
     *
     * @param userIds the user ids to summarize
     * @return the user summaries in the same order as the ids
     */
    private List<UserSummaryResponse> userSummaries(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return userIds.stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(this::toUserSummary)
                .toList();
    }

    private UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .avatar(fileStorageService.avatarUrl(user.getId()))
                .build();
    }
}
