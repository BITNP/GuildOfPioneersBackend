package net.bitnp.guildofpioneers.ticket;

/**
 * Thrown when a registration ticket with the given code has already expired.
 */
public class TicketExpiredException extends RuntimeException {

    public TicketExpiredException(String code) {
        super("Registration ticket with code " + code + " has expired");
    }
}
