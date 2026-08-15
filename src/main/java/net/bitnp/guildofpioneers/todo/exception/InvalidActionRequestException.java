package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when an action creation or update request is invalid, such as when a
 * referenced user does not exist or an assignee does not belong to the owning task.
 */
public class InvalidActionRequestException extends RuntimeException {

    public InvalidActionRequestException(String message) {
        super(message);
    }
}
