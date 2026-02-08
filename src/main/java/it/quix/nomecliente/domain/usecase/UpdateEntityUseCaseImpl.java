package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.config.security.SecurityContextHelper;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.dto.UpdateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.exception.ResourceNotFoundException;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of UpdateEntityUseCase.
 * Contains business logic for updating an entity.
 */
@Service
@RequiredArgsConstructor
public class UpdateEntityUseCaseImpl implements UpdateEntityUseCase {

    private final EntityRepositoryOut entityRepository;
    private final SecurityContextHelper securityContextHelper;

    @Override
    public EntityDTO execute(UUID id, UpdateEntityRequestDTO request) {
        // 1. Load entity by ID
        Entity entity = entityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entity not found with id: " + id));

        // 2. Get authenticated user
        String currentUsername = securityContextHelper.getCurrentUsername();

        // 3. Update fields
        entity.setCode(request.getCode().trim());
        entity.setDescription(request.getDescription().trim());

        // 4. Update audit fields
        entity.setLastUpdateDate(Instant.now());
        entity.setLastUpdateUser(currentUsername);

        // 4. Persist changes
        Entity updatedEntity = entityRepository.update(entity);

        // 5. Return DTO
        return mapToDto(updatedEntity);
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

