package net.bitnp.guildofpioneers.exception;

/**
 * Thrown when an uploaded file is missing, empty, or of an unsupported type.
 */
public class InvalidFileTypeException extends RuntimeException {

    public InvalidFileTypeException(String message) {
        super(message);
    }
}
