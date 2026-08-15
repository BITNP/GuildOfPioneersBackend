package net.bitnp.guildofpioneers.todo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoints for the todo hierarchy: projects, tasks, and actions.
 * Projects are readable by any authenticated user and editable by their leaders;
 * members of the ADMIN department may edit any project. Projects may be created
 * and given covers by managers.
 */
@RestController
@RequestMapping("/api/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * Lists all projects, newest first.
     */
    @GetMapping("/projects")
    public List<TodoProjectResponse> listProjects() {
        return todoService.listProjects();
    }

    /**
     * Creates a project with its leaders and members. Only managers may create
     * projects.
     *
     * @param request        the validated project data
     * @param authentication the current authentication
     * @return the created project, without resolved user summaries
     */
    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public TodoProjectUpdateResponse createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication
    ) {
        return todoService.createProject(request, authentication);
    }

    /**
     * Stores the cover image of a project. Only managers may set a project's cover.
     *
     * @param projectId      the project id
     * @param file           the uploaded cover image
     * @param authentication the current authentication
     * @return the updated project, without resolved user summaries
     */
    @PutMapping("/projects/{projectId}/cover")
    public TodoProjectUpdateResponse uploadProjectCover(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return todoService.uploadProjectCover(projectId, file, authentication);
    }

    /**
     * Returns a single project.
     *
     * @param projectId the project id
     */
    @GetMapping("/projects/{projectId}")
    public TodoProjectResponse getProject(@PathVariable Long projectId) {
        return todoService.getProject(projectId);
    }

    /**
     * Updates a project's title and description. Project leaders are allowed,
     * as is any user in the ADMIN department.
     *
     * @param projectId      the project id
     * @param request        the validated title and description
     * @param authentication the current authentication
     * @return the updated project, without resolved user summaries
     */
    @PutMapping("/projects/{projectId}")
    public TodoProjectUpdateResponse updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        return todoService.updateProject(projectId, request, authentication);
    }

    /**
     * Lists the tasks of a project, newest first.
     *
     * @param projectId the owning project's id
     */
    @GetMapping("/tasks")
    public List<TodoTaskResponse> listTasks(@RequestParam Long projectId) {
        return todoService.listTasks(projectId);
    }

    /**
     * Returns a single task.
     *
     * @param taskId the task id
     */
    @GetMapping("/tasks/{taskId}")
    public TodoTaskResponse getTask(@PathVariable Long taskId) {
        return todoService.getTask(taskId);
    }

    /**
     * Lists the actions of a task, newest first.
     *
     * @param taskId the owning task's id
     */
    @GetMapping("/actions")
    public List<TodoActionResponse> listActions(@RequestParam Long taskId) {
        return todoService.listActions(taskId);
    }

    /**
     * Returns a single action.
     *
     * @param actionId the action id
     */
    @GetMapping("/actions/{actionId}")
    public TodoActionResponse getAction(@PathVariable Long actionId) {
        return todoService.getAction(actionId);
    }
}
