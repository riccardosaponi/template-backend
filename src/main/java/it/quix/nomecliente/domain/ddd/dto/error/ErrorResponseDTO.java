package it.quix.nomecliente.domain.ddd.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard error response DTO.
 * Follows the error model defined in docs/error-model.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {

    /**
     * Stable error code for client handling (e.g., RESOURCE_NOT_FOUND, VALIDATION_ERROR).
     */
    private String code;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Optional detailed error information (e.g., field validation errors).
     */
    private List<ErrorDetailDTO> details;

    /**
     * Optional correlation ID for request tracking.
     */
    private String correlationId;

    /**
     * Factory method for simple errors.
     */
    public static ErrorResponseDTO of(String code, String message) {
        return ErrorResponseDTO.builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Factory method for errors with details.
     */
    public static ErrorResponseDTO of(String code, String message, List<ErrorDetailDTO> details) {
        return ErrorResponseDTO.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();
    }

    /**
     * Factory method for errors with correlation ID.
     */
    public static ErrorResponseDTO of(String code, String message, String correlationId) {
        return ErrorResponseDTO.builder()
                .code(code)
                .message(message)
                .correlationId(correlationId)
                .build();
    }
}

