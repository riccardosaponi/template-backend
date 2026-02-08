package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import org.springframework.data.domain.Page;

/**
 * IN port for listing generic entities.
 */
public interface ListEntitiesIn {
    /**
     * List all entities with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (asc/desc)
     * @return page of entities
     */
    Page<EntityDTO> listEntities(int page, int size, String sortBy, String sortDirection);
}

