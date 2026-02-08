package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CreateEntityUseCase.
 * Tests entity creation with database persistence.
 */
class CreateEntityUseCaseIT extends BaseUseCaseIT {

    @Autowired
    private CreateEntityUseCase createEntityUseCase;

    @Autowired
    private GetEntityUseCase getEntityUseCase;

    @Test
    void execute_shouldCreateEntity_whenValidRequest() {
        // Given
        CreateEntityRequestDTO request = CreateEntityRequestDTO.builder()
                .code("ENT001")
                .description("Test Entity")
                .build();

        // When
        EntityDTO result = createEntityUseCase.execute(request);

        // Then - verifica tutti i campi dell'entità creata
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCode()).isEqualTo("ENT001");
        assertThat(result.getDescription()).isEqualTo("Test Entity");
        assertThat(result.getCreateDate()).isNotNull();
        assertThat(result.getCreateUser()).isEqualTo("test-user");
        assertThat(result.getCanceled()).isFalse();

        // Verifica che sia stato salvato nel database
        EntityDTO retrieved = getEntityUseCase.execute(result.getId());
        assertThat(retrieved.getCode()).isEqualTo("ENT001");
        assertThat(retrieved.getDescription()).isEqualTo("Test Entity");
    }

    @Test
    void execute_shouldTrimFields_whenCreating() {
        // Given - request con spazi
        CreateEntityRequestDTO request = CreateEntityRequestDTO.builder()
                .code("  ENT002  ")
                .description("  Test Description  ")
                .build();

        // When
        EntityDTO result = createEntityUseCase.execute(request);

        // Then - campi normalizzati
        assertThat(result.getCode()).isEqualTo("ENT002");
        assertThat(result.getDescription()).isEqualTo("Test Description");
    }
}

