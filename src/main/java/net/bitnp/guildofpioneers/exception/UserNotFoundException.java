package net.bitnp.guildofpioneers.exception;

/**
 * Thrown when a user cannot be found for a given identifier.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String phone) {
        super("User with phone " + phone + " does not exist");
    }
}
