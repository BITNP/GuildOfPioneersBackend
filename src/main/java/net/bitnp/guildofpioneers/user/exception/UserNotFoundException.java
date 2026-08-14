package net.bitnp.guildofpioneers.user.exception;

/**
 * Thrown when a user cannot be found for a given identifier.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String phone) {
        super("User with phone " + phone + " does not exist");
    }

    public UserNotFoundException(Long id) {
        super("User with id " + id + " does not exist");
    }
}
