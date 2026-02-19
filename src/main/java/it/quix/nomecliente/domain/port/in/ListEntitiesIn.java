package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * IN port for listing generic entities with pagination and sorting.
 *
 * <p>Sort fields are validated by the REST adapter against
 * {@link it.quix.nomecliente.domain.ddd.enumeration.EntitySortField}
 * before the {@code Pageable} is forwarded to the use case.
 */
public interface ListEntitiesIn {

    /**
     * List all entities with pagination and sorting.
     *
     * @param pageable pagination + sort parameters (sort fields pre-validated by the adapter)
     * @return page of entities
     */
    Page<EntityDTO> listEntities(Pageable pageable);
}
