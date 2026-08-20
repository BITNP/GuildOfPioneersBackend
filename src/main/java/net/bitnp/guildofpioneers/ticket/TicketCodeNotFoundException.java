package net.bitnp.guildofpioneers.ticket;

/**
 * Thrown when a ticket code is looked up for validation and no ticket with that
 * code exists.
 */
public class TicketCodeNotFoundException extends RuntimeException {

    public TicketCodeNotFoundException(String code) {
        super("Registration ticket with code " + code + " does not exist");
    }
}
