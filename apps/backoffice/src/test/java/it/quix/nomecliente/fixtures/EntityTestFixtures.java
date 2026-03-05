package it.quix.nomecliente.fixtures;

import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.usecase.CreateEntityUseCase;
import it.quix.nomecliente.domain.usecase.GetEntityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixtures for Entity integration tests.
 * Provides helper methods to create test data.
 */
@Component
@RequiredArgsConstructor
public class EntityTestFixtures {

    private final CreateEntityUseCase createEntityUseCase;
    private final GetEntityUseCase getEntityUseCase;

    /**
     * Crea un'entità con code e description specificati.
     *
     * @param code il codice dell'entità
     * @param description la descrizione dell'entità
     * @return l'EntityDTO creato
     */
    public EntityDTO createEntity(String code, String description) {
        CreateEntityRequestDTO request = CreateEntityRequestDTO.builder()
                .code(code)
                .description(description)
                .build();
        return createEntityUseCase.execute(request);
    }

    /**
     * Crea un'entità con code generato automaticamente.
     *
     * @param description la descrizione dell'entità
     * @return l'EntityDTO creato
     */
    public EntityDTO createEntity(String description) {
        String code = "TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return createEntity(code, description);
    }

    /**
     * Verifica che un'entità esista nel database.
     *
     * @param id l'ID dell'entità
     */
    public void assertEntityExists(UUID id) {
        EntityDTO entity = getEntityUseCase.execute(id);
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(id);
    }
}

