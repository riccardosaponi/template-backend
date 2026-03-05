package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use case for listing generic entities.
 */
public interface ListEntitiesUseCase {
    /**
     * List all entities with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return page of entities
     */
    Page<EntityDTO> execute(Pageable pageable);
}

