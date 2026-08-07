package net.bitnp.guildofpioneers.user.exception;

/**
 * Thrown when attempting to register a phone number that is already in use.
 */
public class PhoneAlreadyExistsException extends RuntimeException {

    public PhoneAlreadyExistsException(String phone) {
        super("Phone " + phone + " is already registered");
    }
}
