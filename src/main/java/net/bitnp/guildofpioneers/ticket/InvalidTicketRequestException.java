package net.bitnp.guildofpioneers.ticket;

/**
 * Thrown when a registration ticket request is rejected because of its contents,
 * for example when the requested department is the ADMIN department.
 */
public class InvalidTicketRequestException extends RuntimeException {

    public InvalidTicketRequestException(String message) {
        super(message);
    }
}
