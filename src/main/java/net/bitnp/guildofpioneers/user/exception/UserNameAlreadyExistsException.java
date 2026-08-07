package net.bitnp.guildofpioneers.user.exception;

/**
 * Thrown when attempting to register a username that is already in use.
 */
public class UserNameAlreadyExistsException extends RuntimeException {

    public UserNameAlreadyExistsException(String username) {
        super("Username " + username + " is already registered");
    }
}
