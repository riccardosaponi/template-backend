package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;

/**
 * Use case for creating a generic entity.
 */
public interface CreateEntityUseCase {
    /**
     * Create a new generic entity.
     *
     * @param request the entity creation request
     * @return the created entity
     */
    EntityDTO execute(CreateEntityRequestDTO request);
}

