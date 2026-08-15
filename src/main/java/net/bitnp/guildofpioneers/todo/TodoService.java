package net.bitnp.guildofpioneers.todo;

import lombok.extern.slf4j.Slf4j;
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
import net.bitnp.guildofpioneers.user.exception.PermissionDeniedException;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
     * Creates a project with its leaders and members. Only a manager may create
     * projects. The creating user is always added as a leader of the project. A user
     * may not be both a leader and a member, and every referenced user must exist.
     *
     * @param request        the validated project data
     * @param authentication the current authentication
     * @return the created project, with member ids but without resolved user summaries
     * @throws PermissionDeniedException       if the current user is not a manager
     * @throws InvalidProjectRequestException  if a user is both leader and member, or a referenced user does not exist
     */
    @Transactional
    public TodoProjectUpdateResponse createProject(
            CreateProjectRequest request, Authentication authentication
    ) {
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isManager(user)) {
            throw new PermissionDeniedException("Only managers can create projects");
        }
        List<Long> memberIds = distinctIds(request.getMemberIds());
        List<Long> leaderIds = new ArrayList<>(distinctIds(request.getLeaderIds()));
        if (!leaderIds.contains(user.getId())) {
            leaderIds.add(user.getId());
        }
        if (leaderIds.stream().anyMatch(memberIds::contains)) {
            throw new InvalidProjectRequestException("A user cannot be both a leader and a member of the same project");
        }
        requireUsersExist(leaderIds, memberIds);

        Instant now = Instant.now();
        TodoProject project = todoProjectRepository.save(TodoProject.builder()
                .title(request.getTitle())
                .description(blankToNull(request.getDescription()))
                .createdDate(now)
                .updatedDate(now)
                .build());
        leaderIds.forEach(userId -> todoProjectLeaderRepository.save(TodoProjectLeader.builder()
                .id(new TodoProjectLeaderKey(project.getId(), userId))
                .build()));
        memberIds.forEach(userId -> todoProjectMemberRepository.save(TodoProjectMember.builder()
                .id(new TodoProjectMemberKey(project.getId(), userId))
                .build()));
        log.trace("Project {} created by {}", project.getId(), authentication.getName());
        return toProjectUpdateResponse(project);
    }

    /**
     * Stores the cover image of a project. A leader of the project may set or
     * replace its cover, as may any admin; the change bumps the project's updated date.
     *
     * @param projectId      the project id
     * @param file           the uploaded cover image
     * @param authentication the current authentication
     * @return the updated project, with member ids but without resolved user summaries
     * @throws TodoProjectNotFoundException if the project does not exist
     * @throws PermissionDeniedException    if the current user is neither a project leader nor an admin
     */
    @Transactional
    public TodoProjectUpdateResponse uploadProjectCover(
            Long projectId, MultipartFile file, Authentication authentication
    ) {
        findProject(projectId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isProjectLeader(projectId, user))) {
            throw new PermissionDeniedException("Only a project leader or admin can set project covers");
        }
        fileStorageService.storeProjectCover(file, projectId);
        touchProject(projectId);
        log.trace("Cover of project {} updated by {}", projectId, authentication.getName());
        return toProjectUpdateResponse(findProject(projectId));
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        unique.removeIf(Objects::isNull);
        return List.copyOf(unique);
    }

    private void requireUsersExist(List<Long> leaderIds, List<Long> memberIds) {
        List<Long> allIds = new ArrayList<>(leaderIds);
        allIds.addAll(memberIds);
        if (allIds.isEmpty()) {
            return;
        }
        long existing = userRepository.findAllById(allIds).size();
        if (existing != allIds.size()) {
            throw new InvalidProjectRequestException("One or more referenced users do not exist");
        }
    }

    private void requireTaskUsersInProject(Long projectId, List<Long> leaderIds, List<Long> memberIds) {
        List<Long> allIds = new ArrayList<>(leaderIds);
        allIds.addAll(memberIds);
        if (allIds.isEmpty()) {
            return;
        }
        boolean allInProject = allIds.stream()
                .allMatch(userId -> isProjectLeader(projectId, userId) || isProjectMember(projectId, userId));
        if (!allInProject) {
            throw new InvalidTaskRequestException("Task leaders and members must belong to the owning project");
        }
    }

    private void requireActionUsersInProject(Long projectId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        boolean allInProject = memberIds.stream()
                .allMatch(userId -> isProjectLeader(projectId, userId) || isProjectMember(projectId, userId));
        if (!allInProject) {
            throw new InvalidActionRequestException("Action members must belong to the owning project");
        }
    }

    /**
     * Ensures every given user is a member of the task, adding them as task
     * members when they are not already a leader or member of the task.
     *
     * @param taskId    the task id
     * @param memberIds the user ids to guarantee as task members
     */
    private void ensureTaskMembers(Long taskId, List<Long> memberIds) {
        Set<Long> current = new HashSet<>(taskLeaderIds(taskId));
        current.addAll(taskMemberIds(taskId));
        memberIds.stream()
                .filter(userId -> !current.contains(userId))
                .forEach(userId -> todoTaskMemberRepository.save(TodoTaskMember.builder()
                        .id(new TodoTaskMemberKey(taskId, userId))
                        .build()));
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
     * Creates a task under a project with its leaders and members. Any member of
     * the owning project (leader or member) may create a task, as may any admin.
     * When the creating user belongs to the owning project they are added as a
     * leader of the task. A user may not be both a leader and a member, and every
     * task leader and member must belong to the owning project.
     *
     * @param request        the validated task data
     * @param authentication the current authentication
     * @return the created task, with member ids but without resolved user summaries
     * @throws TodoProjectNotFoundException if the owning project does not exist
     * @throws PermissionDeniedException    if the current user is not a project member and not an admin
     * @throws InvalidTaskRequestException  if a user is both leader and member, or an assignee is not a member of the owning project
     */
    @Transactional
    public TodoTaskUpdateResponse createTask(
            CreateTaskRequest request, Authentication authentication
    ) {
        Long projectId = request.getProjectId();
        findProject(projectId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isProjectMemberOrLeader(projectId, user))) {
            throw new PermissionDeniedException("Only members of the project can create tasks");
        }
        List<Long> memberIds = distinctIds(request.getMemberIds());
        List<Long> leaderIds = new ArrayList<>(distinctIds(request.getLeaderIds()));
        if (!leaderIds.contains(user.getId()) && isProjectMemberOrLeader(projectId, user)) {
            leaderIds.add(user.getId());
        }
        if (leaderIds.stream().anyMatch(memberIds::contains)) {
            throw new InvalidTaskRequestException("A user cannot be both a leader and a member of the same task");
        }
        requireTaskUsersInProject(projectId, leaderIds, memberIds);

        Instant now = Instant.now();
        TodoTask task = todoTaskRepository.save(TodoTask.builder()
                .projectId(projectId)
                .title(request.getTitle())
                .description(blankToNull(request.getDescription()))
                .createdDate(now)
                .updatedDate(now)
                .build());
        leaderIds.forEach(userId -> todoTaskLeaderRepository.save(TodoTaskLeader.builder()
                .id(new TodoTaskLeaderKey(task.getId(), userId))
                .build()));
        memberIds.forEach(userId -> todoTaskMemberRepository.save(TodoTaskMember.builder()
                .id(new TodoTaskMemberKey(task.getId(), userId))
                .build()));
        touchProject(projectId);
        log.trace("Task {} created by {}", task.getId(), authentication.getName());
        return toTaskUpdateResponse(task);
    }

    /**
     * Updates a task's title and description, optionally replacing its leaders
     * and members. Only a leader of the task may edit it, unless the current user
     * is an admin, in which case any task may be edited; the change bumps the
     * task's updated date and propagates up to the owning project. When
     * {@code leaderIds} or {@code memberIds} is provided, the corresponding
     * membership list is replaced entirely; when absent it is left unchanged.
     * Every task leader and member must belong to the owning project.
     *
     * @param taskId         the task id
     * @param request        the validated title, description, and membership lists
     * @param authentication the current authentication
     * @return the updated task, with member ids but without resolved user summaries
     * @throws TodoTaskNotFoundException if the task does not exist
     * @throws PermissionDeniedException  if the current user is neither a task leader nor an admin
     * @throws InvalidTaskRequestException if a user is both leader and member, or an assignee is not a member of the owning project
     */
    @Transactional
    public TodoTaskUpdateResponse updateTask(
            Long taskId, UpdateTaskRequest request, Authentication authentication
    ) {
        TodoTask task = findTask(taskId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isTaskLeader(taskId, user))) {
            throw new PermissionDeniedException("Only a leader of the task can edit it");
        }
        if (request.getLeaderIds() != null || request.getMemberIds() != null) {
            List<Long> leaderIds = request.getLeaderIds() != null
                    ? distinctIds(request.getLeaderIds())
                    : taskLeaderIds(taskId);
            List<Long> memberIds = request.getMemberIds() != null
                    ? distinctIds(request.getMemberIds())
                    : taskMemberIds(taskId);
            if (leaderIds.stream().anyMatch(memberIds::contains)) {
                throw new InvalidTaskRequestException("A user cannot be both a leader and a member of the same task");
            }
            requireTaskUsersInProject(task.getProjectId(), leaderIds, memberIds);
            replaceTaskMembers(taskId, leaderIds, memberIds);
        }
        task.setTitle(request.getTitle());
        task.setDescription(blankToNull(request.getDescription()));
        task.setUpdatedDate(Instant.now());
        todoTaskRepository.save(task);
        touchProject(task.getProjectId());
        log.trace("Task {} updated", taskId);
        return toTaskUpdateResponse(task);
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
     * Creates an action under a task with its members. Any member of the owning
     * task (leader or member) may create an action, as may any admin. When the
     * creating user belongs to the owning project they are added as a member of
     * the action. Every action member must belong to the owning project. Members
     * who are not already part of the owning task are added to it as task members.
     *
     * @param request        the validated action data
     * @param authentication the current authentication
     * @return the created action response
     * @throws TodoTaskNotFoundException       if the owning task does not exist
     * @throws PermissionDeniedException       if the current user is not a task member and not an admin
     * @throws InvalidActionRequestException   if an assignee is not a member of the owning project
     */
    @Transactional
    public TodoActionResponse createAction(
            CreateActionRequest request, Authentication authentication
    ) {
        Long taskId = request.getTaskId();
        TodoTask task = findTask(taskId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isTaskMemberOrLeader(taskId, user))) {
            throw new PermissionDeniedException("Only members of the task can create actions");
        }
        List<Long> memberIds = new ArrayList<>(distinctIds(request.getMemberIds()));
        if (!memberIds.contains(user.getId()) && isProjectMemberOrLeader(task.getProjectId(), user)) {
            memberIds.add(user.getId());
        }
        requireActionUsersInProject(task.getProjectId(), memberIds);

        Instant now = Instant.now();
        TodoAction action = todoActionRepository.save(TodoAction.builder()
                .taskId(taskId)
                .title(request.getTitle())
                .description(blankToNull(request.getDescription()))
                .createdDate(now)
                .updatedDate(now)
                .build());
        memberIds.forEach(userId -> todoActionMemberRepository.save(TodoActionMember.builder()
                .id(new TodoActionMemberKey(action.getId(), userId))
                .build()));
        ensureTaskMembers(taskId, memberIds);
        touchTask(taskId);
        log.trace("Action {} created by {}", action.getId(), authentication.getName());
        return toActionResponse(action);
    }

    /**
     * Updates an action's title and description, optionally replacing its
     * members. Only a member of the action may edit it, unless the current user
     * is an admin, in which case any action may be edited; the change bumps the
     * action's updated date and propagates up to the owning task and project.
     * When {@code memberIds} is provided, the membership list is replaced
     * entirely; when absent it is left unchanged. Every action member must
     * belong to the owning project. Members who are not already part of the
     * owning task are added to it as task members.
     *
     * @param actionId       the action id
     * @param request        the validated title, description, and member list
     * @param authentication the current authentication
     * @return the updated action response
     * @throws TodoActionNotFoundException    if the action does not exist
     * @throws PermissionDeniedException      if the current user is neither an action member nor an admin
     * @throws InvalidActionRequestException  if an assignee is not a member of the owning project
     */
    @Transactional
    public TodoActionResponse updateAction(
            Long actionId, UpdateActionRequest request, Authentication authentication
    ) {
        TodoAction action = findAction(actionId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isActionMember(actionId, user))) {
            throw new PermissionDeniedException("Only a member of the action can edit it");
        }
        if (request.getMemberIds() != null) {
            List<Long> memberIds = distinctIds(request.getMemberIds());
            TodoTask task = findTask(action.getTaskId());
            requireActionUsersInProject(task.getProjectId(), memberIds);
            replaceActionMembers(actionId, memberIds);
            ensureTaskMembers(task.getId(), memberIds);
        }
        action.setTitle(request.getTitle());
        action.setDescription(blankToNull(request.getDescription()));
        action.setUpdatedDate(Instant.now());
        todoActionRepository.save(action);
        touchTask(action.getTaskId());
        log.trace("Action {} updated", actionId);
        return toActionResponse(action);
    }

    /**
     * Marks an action as finished by setting its end date to the current time
     * and bumping its updated date, which propagates to the owning task and
     * project. Only a member of the action may finish it, unless the current
     * user is an admin.
     *
     * @param actionId       the action id
     * @param authentication the current authentication
     * @return the updated action response
     * @throws TodoActionNotFoundException if the action does not exist
     * @throws PermissionDeniedException   if the current user is neither an action member nor an admin
     */
    @Transactional
    public TodoActionResponse finishAction(Long actionId, Authentication authentication) {
        TodoAction action = findAction(actionId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isActionMember(actionId, user))) {
            throw new PermissionDeniedException("Only a member of the action can finish it");
        }
        action.setEndDate(Instant.now());
        action.setUpdatedDate(Instant.now());
        todoActionRepository.save(action);
        touchTask(action.getTaskId());
        log.trace("Action {} finished", actionId);
        return toActionResponse(action);
    }

    /**
     * Reopens a finished action by clearing its end date and bumping its updated
     * date, which propagates to the owning task and project. Only a member of
     * the action may reopen it, unless the current user is an admin.
     *
     * @param actionId       the action id
     * @param authentication the current authentication
     * @return the updated action response
     * @throws TodoActionNotFoundException if the action does not exist
     * @throws PermissionDeniedException   if the current user is neither an action member nor an admin
     */
    @Transactional
    public TodoActionResponse unfinishAction(Long actionId, Authentication authentication) {
        TodoAction action = findAction(actionId);
        User user = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(user, () -> isActionMember(actionId, user))) {
            throw new PermissionDeniedException("Only a member of the action can reopen it");
        }
        action.setEndDate(null);
        action.setUpdatedDate(Instant.now());
        todoActionRepository.save(action);
        touchTask(action.getTaskId());
        log.trace("Action {} reopened", actionId);
        return toActionResponse(action);
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
     * Updates a project's title and description, optionally replacing its leaders
     * and members. Only a leader of the project may edit it, unless the current
     * user is an admin, in which case any project may be edited; the change bumps
     * the project's updated date. When {@code leaderIds} or {@code memberIds} is
     * provided, the corresponding membership list is replaced entirely; when
     * absent it is left unchanged.
     *
     * @param projectId      the project id
     * @param request        the validated title, description, and membership lists
     * @param authentication the current authentication
     * @return the updated project, with member ids but without resolved user summaries
     * @throws TodoProjectNotFoundException    if the project does not exist
     * @throws NotProjectLeaderException       if the current user is not a project leader
     * @throws InvalidProjectRequestException  if a user is both leader and member, or a referenced user does not exist
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
        if (request.getLeaderIds() != null || request.getMemberIds() != null) {
            List<Long> leaderIds = request.getLeaderIds() != null
                    ? distinctIds(request.getLeaderIds())
                    : projectLeaderIds(projectId);
            List<Long> memberIds = request.getMemberIds() != null
                    ? distinctIds(request.getMemberIds())
                    : projectMemberIds(projectId);
            if (leaderIds.stream().anyMatch(memberIds::contains)) {
                throw new InvalidProjectRequestException("A user cannot be both a leader and a member of the same project");
            }
            requireUsersExist(leaderIds, memberIds);
            replaceProjectMembers(projectId, leaderIds, memberIds);
        }
        project.setTitle(request.getTitle());
        project.setDescription(blankToNull(request.getDescription()));
        project.setUpdatedDate(Instant.now());
        log.trace("Project {} updated", projectId);
        return toProjectUpdateResponse(todoProjectRepository.save(project));
    }

    private void replaceProjectMembers(Long projectId, List<Long> leaderIds, List<Long> memberIds) {
        todoProjectLeaderRepository.deleteAll(todoProjectLeaderRepository.findById_ProjectId(projectId));
        todoProjectMemberRepository.deleteAll(todoProjectMemberRepository.findById_ProjectId(projectId));
        leaderIds.forEach(userId -> todoProjectLeaderRepository.save(TodoProjectLeader.builder()
                .id(new TodoProjectLeaderKey(projectId, userId))
                .build()));
        memberIds.forEach(userId -> todoProjectMemberRepository.save(TodoProjectMember.builder()
                .id(new TodoProjectMemberKey(projectId, userId))
                .build()));
    }

    private void replaceTaskMembers(Long taskId, List<Long> leaderIds, List<Long> memberIds) {
        todoTaskLeaderRepository.deleteAll(todoTaskLeaderRepository.findById_TaskId(taskId));
        todoTaskMemberRepository.deleteAll(todoTaskMemberRepository.findById_TaskId(taskId));
        leaderIds.forEach(userId -> todoTaskLeaderRepository.save(TodoTaskLeader.builder()
                .id(new TodoTaskLeaderKey(taskId, userId))
                .build()));
        memberIds.forEach(userId -> todoTaskMemberRepository.save(TodoTaskMember.builder()
                .id(new TodoTaskMemberKey(taskId, userId))
                .build()));
    }

    private void replaceActionMembers(Long actionId, List<Long> memberIds) {
        todoActionMemberRepository.deleteAll(todoActionMemberRepository.findById_ActionId(actionId));
        memberIds.forEach(userId -> todoActionMemberRepository.save(TodoActionMember.builder()
                .id(new TodoActionMemberKey(actionId, userId))
                .build()));
    }

    private boolean isProjectLeader(Long projectId, User user) {
        return isProjectLeader(projectId, user.getId());
    }

    private boolean isProjectLeader(Long projectId, Long userId) {
        return todoProjectLeaderRepository.existsById(new TodoProjectLeaderKey(projectId, userId));
    }

    private boolean isProjectMember(Long projectId, Long userId) {
        return todoProjectMemberRepository.existsById(new TodoProjectMemberKey(projectId, userId));
    }

    private boolean isProjectMemberOrLeader(Long projectId, User user) {
        return isProjectLeader(projectId, user.getId())
                || isProjectMember(projectId, user.getId());
    }

    private boolean isTaskLeader(Long taskId, User user) {
        return todoTaskLeaderRepository.existsById(new TodoTaskLeaderKey(taskId, user.getId()));
    }

    private boolean isTaskLeader(Long taskId, Long userId) {
        return todoTaskLeaderRepository.existsById(new TodoTaskLeaderKey(taskId, userId));
    }

    private boolean isTaskMember(Long taskId, Long userId) {
        return todoTaskMemberRepository.existsById(new TodoTaskMemberKey(taskId, userId));
    }

    private boolean isTaskMemberOrLeader(Long taskId, User user) {
        return isTaskLeader(taskId, user) || isTaskMember(taskId, user.getId());
    }

    private boolean isActionMember(Long actionId, User user) {
        return todoActionMemberRepository.existsById(new TodoActionMemberKey(actionId, user.getId()));
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

    private TodoTaskUpdateResponse toTaskUpdateResponse(TodoTask task) {
        return TodoTaskUpdateResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .createdDate(task.getCreatedDate())
                .updatedDate(task.getUpdatedDate())
                .endDate(task.getEndDate())
                .leaderIds(taskLeaderIds(task.getId()))
                .memberIds(taskMemberIds(task.getId()))
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
