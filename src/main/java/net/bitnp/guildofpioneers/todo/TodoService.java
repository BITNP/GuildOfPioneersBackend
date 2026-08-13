package net.bitnp.guildofpioneers.todo;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.storage.FileStorageService;
import net.bitnp.guildofpioneers.todo.entity.TodoAction;
import net.bitnp.guildofpioneers.todo.entity.TodoProject;
import net.bitnp.guildofpioneers.todo.entity.TodoTask;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Provides read access to the three-layer todo hierarchy: projects, tasks, and actions.
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

    public TodoService(
            TodoProjectRepository todoProjectRepository,
            TodoTaskRepository todoTaskRepository,
            TodoActionRepository todoActionRepository,
            TodoProjectLeaderRepository todoProjectLeaderRepository,
            TodoProjectMemberRepository todoProjectMemberRepository,
            TodoTaskLeaderRepository todoTaskLeaderRepository,
            TodoTaskMemberRepository todoTaskMemberRepository,
            TodoActionMemberRepository todoActionMemberRepository,
            FileStorageService fileStorageService
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
    }

    /**
     * Returns all projects ordered by creation date, newest first.
     *
     * @return the project responses
     */
    @Transactional(readOnly = true)
    public List<TodoProjectResponse> listProjects() {
        return todoProjectRepository.findAllByOrderByCreatedDateDesc().stream()
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
     * Returns the tasks of a project ordered by creation date, newest first.
     *
     * @param projectId the owning project's id
     * @return the task responses
     * @throws TodoProjectNotFoundException if the project does not exist
     */
    @Transactional(readOnly = true)
    public List<TodoTaskResponse> listTasks(Long projectId) {
        findProject(projectId);
        return todoTaskRepository.findByProjectIdOrderByCreatedDateDesc(projectId).stream()
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
     * Returns the actions of a task ordered by creation date, newest first.
     *
     * @param taskId the owning task's id
     * @return the action responses
     * @throws TodoTaskNotFoundException if the task does not exist
     */
    @Transactional(readOnly = true)
    public List<TodoActionResponse> listActions(Long taskId) {
        findTask(taskId);
        return todoActionRepository.findByTaskIdOrderByCreatedDateDesc(taskId).stream()
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
        return TodoProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .cover(fileStorageService.projectCoverUrl(project.getId()))
                .description(project.getDescription())
                .createdDate(project.getCreatedDate())
                .endDate(project.getEndDate())
                .leaderIds(projectLeaderIds(project.getId()))
                .memberIds(projectMemberIds(project.getId()))
                .build();
    }

    private TodoTaskResponse toTaskResponse(TodoTask task) {
        return TodoTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .createdDate(task.getCreatedDate())
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
}
