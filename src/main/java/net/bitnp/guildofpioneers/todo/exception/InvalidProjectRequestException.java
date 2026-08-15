package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a project creation request is invalid, such as when a user appears
 * in both the leader and member lists or a referenced user does not exist.
 */
public class InvalidProjectRequestException extends RuntimeException {

    public InvalidProjectRequestException(String message) {
        super(message);
    }
}
