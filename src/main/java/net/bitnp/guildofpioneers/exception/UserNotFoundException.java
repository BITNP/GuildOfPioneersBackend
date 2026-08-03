package net.bitnp.guildofpioneers.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String phone) {
        super("User with phone " + phone + " does not exist");
    }
}
