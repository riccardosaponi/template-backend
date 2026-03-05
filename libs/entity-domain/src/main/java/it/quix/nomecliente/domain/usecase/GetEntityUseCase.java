package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import java.util.UUID;

/**
 * Use case for retrieving a generic entity by ID.
 */
public interface GetEntityUseCase {
    /**
     * Get entity by ID.
     *
     * @param id the entity ID
     * @return the entity
     */
    EntityDTO execute(UUID id);
}

