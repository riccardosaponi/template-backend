package it.quix.nomecliente.domain.ddd.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic entity domain model.
 * Represents a generic business entity with code, description and audit fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entity {

    private UUID id;
    private String code;
    private String description;
    private Instant createDate;
    private String createUser;
    private Instant lastUpdateDate;
    private String lastUpdateUser;

    @Builder.Default
    private Boolean canceled = false;
}

