package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import org.springframework.http.ResponseEntity;

/**
 * IN port for creating a generic entity.
 */
public interface CreateEntityIn {
    /**
     * Create a new entity.
     *
     * @param request the entity creation request
     * @return HTTP 201 with the created entity
     */
    ResponseEntity<EntityDTO> createEntity(CreateEntityRequestDTO request);
}

