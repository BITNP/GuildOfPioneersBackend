package net.bitnp.guildofpioneers.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String code) {
        super("Registration ticket with code " + code + " does not exist");
    }
}
