package it.quix.nomecliente.domain.exception;

/**
 * Exception thrown when access is denied (403 Forbidden).
 * Use when user is authenticated but lacks required permissions.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}

