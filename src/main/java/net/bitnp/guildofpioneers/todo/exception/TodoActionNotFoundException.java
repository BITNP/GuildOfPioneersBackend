package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a requested action does not exist.
 */
public class TodoActionNotFoundException extends RuntimeException {

    public TodoActionNotFoundException(Long actionId) {
        super("Action with id " + actionId + " not found");
    }
}
