package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a user who is not a leader of a project tries to modify it.
 */
public class NotProjectLeaderException extends RuntimeException {

    public NotProjectLeaderException(Long projectId) {
        super("User is not a leader of project with id " + projectId);
    }
}
