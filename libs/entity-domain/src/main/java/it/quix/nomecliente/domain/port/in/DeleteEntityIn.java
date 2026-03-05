package it.quix.nomecliente.domain.port.in;

import java.util.UUID;

/**
 * IN port for deleting a generic entity.
 */
public interface DeleteEntityIn {
    /**
     * Delete an entity by ID (logical delete).
     *
     * @param id the entity ID
     */
    void deleteEntity(UUID id);
}

