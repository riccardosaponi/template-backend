package it.quix.nomecliente.config;

import it.quix.nomecliente.domain.ddd.dto.error.ErrorDetailDTO;
import it.quix.nomecliente.domain.ddd.dto.error.ErrorResponseDTO;
import it.quix.nomecliente.domain.exception.BusinessRuleViolationException;
import it.quix.nomecliente.domain.exception.ForbiddenException;
import it.quix.nomecliente.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Handles all exceptions and converts them to standard ErrorResponseDTO format.
 * Follows error model defined in docs/error-model.md
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException (404 Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Resource not found [correlationId={}]: {}", correlationId, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle validation errors (400 Bad Request).
     * Triggered by @Valid annotation on request body.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Validation error [correlationId={}]: {} field errors",
                correlationId, ex.getBindingResult().getFieldErrorCount());

        List<ErrorDetailDTO> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .details(details)
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle business rule violations (400 Bad Request or 409 Conflict).
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRuleViolation(
            BusinessRuleViolationException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Business rule violation [correlationId={}]: {}", correlationId, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "BUSINESS_RULE_VIOLATION",
                ex.getMessage(),
                correlationId
        );

        // 409 Conflict se il messaggio contiene "conflict", altrimenti 400
        HttpStatus status = ex.getMessage().toLowerCase().contains("conflict")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handle ForbiddenException (403 Forbidden).
     * User is authenticated but lacks required permissions.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDTO> handleForbiddenException(
            ForbiddenException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Forbidden access [correlationId={}]: {}", correlationId, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "FORBIDDEN",
                ex.getMessage(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle Spring Security AccessDeniedException (403 Forbidden).
     * Thrown by @PreAuthorize when authorization fails.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Access denied [correlationId={}]: {}", correlationId, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "FORBIDDEN",
                "Access denied: insufficient permissions",
                correlationId
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle AuthenticationException (401 Unauthorized).
     * User is not authenticated.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Authentication failed [correlationId={}]: {}", correlationId, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "UNAUTHORIZED",
                "Authentication required",
                correlationId
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle IllegalArgumentException (400 Bad Request).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.warn("Illegal argument [correlationId={}]: {}", correlationId, ex.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                correlationId
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle generic exceptions (500 Internal Server Error).
     * Avoid leaking sensitive information.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            WebRequest request) {

        String correlationId = generateCorrelationId();
        log.error("Unexpected error [correlationId={}]: {}", correlationId, ex.getMessage(), ex);

        ErrorResponseDTO error = ErrorResponseDTO.of(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please contact support with correlation ID: " + correlationId,
                correlationId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Map FieldError to ErrorDetailDTO.
     */
    private ErrorDetailDTO mapFieldError(FieldError fieldError) {
        return ErrorDetailDTO.of(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    /**
     * Generate unique correlation ID for error tracking.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}

