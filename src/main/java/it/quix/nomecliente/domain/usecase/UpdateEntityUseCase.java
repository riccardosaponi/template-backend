package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.dto.UpdateEntityRequestDTO;
import java.util.UUID;

/**
 * Use case for updating a generic entity.
 */
public interface UpdateEntityUseCase {
    /**
     * Update an existing entity.
     *
     * @param id the entity ID
     * @param request the update request
     * @return the updated entity
     */
    EntityDTO execute(UUID id, UpdateEntityRequestDTO request);
}

