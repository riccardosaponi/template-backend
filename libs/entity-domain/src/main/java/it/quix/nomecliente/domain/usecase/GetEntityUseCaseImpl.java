package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.exception.ResourceNotFoundException;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of GetEntityUseCase.
 * Contains business logic for retrieving an entity.
 */
@Service
@RequiredArgsConstructor
public class GetEntityUseCaseImpl implements GetEntityUseCase {

    private final EntityRepositoryOut entityRepository;

    @Override
    public EntityDTO execute(UUID id) {
        // 1. Load entity by ID
        Entity entity = entityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entity not found with id: " + id));

        // 2. Return DTO
        return mapToDto(entity);
    }

    private EntityDTO mapToDto(Entity entity) {
        return EntityDTO.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .description(entity.getDescription())
            .createDate(entity.getCreateDate())
            .createUser(entity.getCreateUser())
            .lastUpdateDate(entity.getLastUpdateDate())
            .lastUpdateUser(entity.getLastUpdateUser())
            .canceled(entity.getCanceled())
            .build();
    }
}

