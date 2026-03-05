package it.quix.nomecliente.domain.ddd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity DTO for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDTO {

    private UUID id;
    private String code;
    private String description;
    private Instant createDate;
    private String createUser;
    private Instant lastUpdateDate;
    private String lastUpdateUser;
    private Boolean canceled;
}

