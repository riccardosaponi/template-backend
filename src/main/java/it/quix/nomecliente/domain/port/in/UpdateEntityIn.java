package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.dto.UpdateEntityRequestDTO;
import java.util.UUID;

/**
 * IN port for updating a generic entity.
 */
public interface UpdateEntityIn {
    /**
     * Update an existing entity.
     *
     * @param id the entity ID
     * @param request the update request
     * @return the updated entity
     */
    EntityDTO updateEntity(UUID id, UpdateEntityRequestDTO request);
}

