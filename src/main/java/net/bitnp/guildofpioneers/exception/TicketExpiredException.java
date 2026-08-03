package net.bitnp.guildofpioneers.exception;

public class TicketExpiredException extends RuntimeException {

    public TicketExpiredException(String code) {
        super("Registration ticket with code " + code + " has expired");
    }
}
