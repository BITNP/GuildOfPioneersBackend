package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a task creation or update request is invalid, such as when a user
 * appears in both the leader and member lists or a referenced user does not exist.
 */
public class InvalidTaskRequestException extends RuntimeException {

    public InvalidTaskRequestException(String message) {
        super(message);
    }
}
