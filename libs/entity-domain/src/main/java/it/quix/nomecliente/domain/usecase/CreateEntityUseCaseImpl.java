package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.config.security.SecurityContextHelper;
import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of CreateEntityUseCase.
 * Contains business logic for entity creation.
 */
@Service
@RequiredArgsConstructor
public class CreateEntityUseCaseImpl implements CreateEntityUseCase {

    private final EntityRepositoryOut entityRepository;
    private final SecurityContextHelper securityContextHelper;

    @Override
    public EntityDTO execute(CreateEntityRequestDTO request) {
        // 1. Validate input (validation is done by @Valid annotations)

        // 2. Get authenticated user
        String currentUsername = securityContextHelper.getCurrentUsername();

        // 3. Create entity
        Entity entity = Entity.builder()
            .id(UUID.randomUUID())
            .code(request.getCode().trim())
            .description(request.getDescription().trim())
            .createDate(Instant.now())
            .createUser(currentUsername)
            .canceled(false)
            .build();

        // 4. Persist entity
        Entity savedEntity = entityRepository.save(entity);

        // 5. Return DTO
        return mapToDto(savedEntity);
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

