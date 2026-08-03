package net.bitnp.guildofpioneers.exception;

public class PhoneAlreadyExistsException extends RuntimeException {

    public PhoneAlreadyExistsException(String phone) {
        super("Phone " + phone + " is already registered");
    }
}
