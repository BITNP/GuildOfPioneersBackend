package net.bitnp.guildofpioneers.user.exception;

/**
 * Thrown when an authenticated user attempts an action they are not allowed to perform.
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
