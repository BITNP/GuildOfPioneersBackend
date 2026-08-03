package net.bitnp.guildofpioneers.exception;

/**
 * Thrown when a registration ticket with the given code does not exist.
 */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String code) {
        super("Registration ticket with code " + code + " does not exist");
    }
}
