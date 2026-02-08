package it.quix.nomecliente.domain.usecase;

import java.util.UUID;

/**
 * Use case for deleting a generic entity (logical delete).
 */
public interface DeleteEntityUseCase {
    /**
     * Delete an entity by ID (logical delete - sets canceled=true).
     *
     * @param id the entity ID
     */
    void execute(UUID id);
}

