package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import java.util.UUID;

/**
 * IN port for getting a generic entity.
 */
public interface GetEntityIn {
    /**
     * Get entity by ID.
     *
     * @param id the entity ID
     * @return the entity
     */
    EntityDTO getEntity(UUID id);
}

