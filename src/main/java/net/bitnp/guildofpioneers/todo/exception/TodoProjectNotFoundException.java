package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a requested project does not exist.
 */
public class TodoProjectNotFoundException extends RuntimeException {

    public TodoProjectNotFoundException(Long projectId) {
        super("Project with id " + projectId + " not found");
    }
}
