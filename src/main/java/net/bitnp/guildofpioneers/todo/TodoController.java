package net.bitnp.guildofpioneers.todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoints for the todo hierarchy: projects, tasks, and actions.
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
     * Returns a single project.
     *
     * @param projectId the project id
     */
    @GetMapping("/projects/{projectId}")
    public TodoProjectResponse getProject(@PathVariable Long projectId) {
        return todoService.getProject(projectId);
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
