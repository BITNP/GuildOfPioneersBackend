package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a requested task does not exist.
 */
public class TodoTaskNotFoundException extends RuntimeException {

    public TodoTaskNotFoundException(Long taskId) {
        super("Task with id " + taskId + " not found");
    }
}
