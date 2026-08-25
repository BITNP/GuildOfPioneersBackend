package net.bitnp.guildofpioneers.todo.exception;

/**
 * Thrown when a todo tree request is invalid, such as when the requested depth
 * falls outside the supported range.
 */
public class InvalidTodoTreeRequestException extends RuntimeException {

    public InvalidTodoTreeRequestException(String message) {
        super(message);
    }
}
