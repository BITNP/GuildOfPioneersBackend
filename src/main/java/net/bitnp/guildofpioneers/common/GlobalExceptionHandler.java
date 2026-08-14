package net.bitnp.guildofpioneers.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.storage.InvalidFileTypeException;
import net.bitnp.guildofpioneers.storage.StoredFileNotFoundException;
import net.bitnp.guildofpioneers.ticket.TicketExpiredException;
import net.bitnp.guildofpioneers.ticket.TicketNotFoundException;
import net.bitnp.guildofpioneers.todo.exception.TodoActionNotFoundException;
import net.bitnp.guildofpioneers.todo.exception.TodoProjectNotFoundException;
import net.bitnp.guildofpioneers.todo.exception.TodoTaskNotFoundException;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handling that maps thrown exceptions to JSON error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({PhoneAlreadyExistsException.class, UserNameAlreadyExistsException.class})
    public ResponseEntity<Map<String, Object>> handleDuplicateRegistration(
            RuntimeException ex, HttpServletRequest request
    ) {
        log.trace("Registration rejected: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler({TicketNotFoundException.class, TicketExpiredException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidTicket(
            RuntimeException ex, HttpServletRequest request
    ) {
        log.trace("Invalid registration ticket: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request
    ) {
        log.trace("Failed login attempt for {} {}", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getField() + " " + fieldError.getDefaultMessage()
                : "Validation failed";
        log.trace("Request rejected due to validation failure: {}", message);
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(
            InvalidFileTypeException ex, HttpServletRequest request
    ) {
        log.trace("Upload rejected: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStoredFileNotFound(
            StoredFileNotFoundException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler({TodoProjectNotFoundException.class, TodoTaskNotFoundException.class, TodoActionNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleTodoNotFound(
            RuntimeException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingFile(
            MissingServletRequestPartException ex, HttpServletRequest request
    ) {
        log.trace("Upload rejected: multipart file is required");
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Multipart file is required", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request
    ) {
        log.trace("Upload rejected: file exceeds maximum allowed size");
        return build(HttpStatus.CONTENT_TOO_LARGE, "CONTENT_TOO_LARGE", "File exceeds maximum allowed size", request);
    }

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String error, String message, HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
