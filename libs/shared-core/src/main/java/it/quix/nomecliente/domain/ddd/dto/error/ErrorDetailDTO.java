package it.quix.nomecliente.domain.ddd.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error detail for field-specific errors (e.g., validation errors).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetailDTO {

    /**
     * Field name that caused the error (optional).
     */
    private String field;

    /**
     * Description of the issue.
     */
    private String issue;

    /**
     * Factory method for field-specific error.
     */
    public static ErrorDetailDTO of(String field, String issue) {
        return ErrorDetailDTO.builder()
                .field(field)
                .issue(issue)
                .build();
    }

    /**
     * Factory method for generic detail without field.
     */
    public static ErrorDetailDTO of(String issue) {
        return ErrorDetailDTO.builder()
                .issue(issue)
                .build();
    }
}

