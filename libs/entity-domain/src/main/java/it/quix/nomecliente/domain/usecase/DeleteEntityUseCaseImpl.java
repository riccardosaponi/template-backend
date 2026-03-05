package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.config.security.SecurityContextHelper;
import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.exception.ResourceNotFoundException;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of DeleteEntityUseCase.
 * Contains business logic for logical deletion (canceled=true).
 */
@Service
@RequiredArgsConstructor
public class DeleteEntityUseCaseImpl implements DeleteEntityUseCase {

    private final EntityRepositoryOut entityRepository;
    private final SecurityContextHelper securityContextHelper;

    @Override
    public void execute(UUID id) {
        // 1. Load entity by ID
        Entity entity = entityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entity not found with id: " + id));

        // 2. Get authenticated user
        String currentUsername = securityContextHelper.getCurrentUsername();

        // 3. Logical delete: set canceled=true
        entity.setCanceled(true);
        entity.setLastUpdateDate(Instant.now());
        entity.setLastUpdateUser(currentUsername);

        // 4. Persist changes
        entityRepository.update(entity);
    }
}

