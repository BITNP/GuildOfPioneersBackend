package net.bitnp.guildofpioneers.auth;

/** Signals a conflicting or already admitted external identity. */
public class IdentityConflictException extends RuntimeException {
    public IdentityConflictException(String message) { super(message); }
}
