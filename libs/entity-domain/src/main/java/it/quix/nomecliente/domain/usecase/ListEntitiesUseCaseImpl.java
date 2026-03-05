package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Implementation of ListEntitiesUseCase.
 * Contains business logic for listing entities.
 */
@Service
@RequiredArgsConstructor
public class ListEntitiesUseCaseImpl implements ListEntitiesUseCase {

    private final EntityRepositoryOut entityRepository;

    @Override
    public Page<EntityDTO> execute(Pageable pageable) {
        // 1. Query entities with pagination
        Page<Entity> entities = entityRepository.findAll(pageable);

        // 2. Map to DTOs
        return entities.map(this::mapToDto);
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

