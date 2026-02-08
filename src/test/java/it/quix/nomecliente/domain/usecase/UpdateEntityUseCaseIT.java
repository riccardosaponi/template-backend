package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.ddd.dto.UpdateEntityRequestDTO;
import it.quix.nomecliente.domain.exception.ResourceNotFoundException;
import it.quix.nomecliente.fixtures.EntityTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for UpdateEntityUseCase.
 * Tests entity update with database persistence and audit trail.
 */
class UpdateEntityUseCaseIT extends BaseUseCaseIT {

    @Autowired
    private UpdateEntityUseCase updateEntityUseCase;

    @Autowired
    private GetEntityUseCase getEntityUseCase;

    @Autowired
    private EntityTestFixtures fixtures;

    @Test
    void execute_shouldUpdateEntity_whenValidRequest() {
        // Given - creo un'entità con valori iniziali
        EntityDTO created = fixtures.createEntity("UPD-001", "Original Description");

        // When - aggiorno code e description
        UpdateEntityRequestDTO updateRequest = UpdateEntityRequestDTO.builder()
                .code("UPD-001-UPDATED")
                .description("Updated Description")
                .build();

        EntityDTO updated = updateEntityUseCase.execute(created.getId(), updateRequest);

        // Then - verifica che i campi siano stati aggiornati
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getCode()).isEqualTo("UPD-001-UPDATED");
        assertThat(updated.getDescription()).isEqualTo("Updated Description");

        // Verifica audit fields
        assertThat(updated.getCreateDate()).isEqualTo(created.getCreateDate());
        assertThat(updated.getCreateUser()).isEqualTo(created.getCreateUser());
        assertThat(updated.getLastUpdateDate()).isNotNull();
        assertThat(updated.getLastUpdateUser()).isEqualTo("test-user");

        // Verifica persistenza nel database
        EntityDTO retrieved = getEntityUseCase.execute(created.getId());
        assertThat(retrieved.getCode()).isEqualTo("UPD-001-UPDATED");
        assertThat(retrieved.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void execute_shouldThrowException_whenEntityNotFound() {
        // Given - un ID che non esiste
        UUID nonExistentId = UUID.randomUUID();
        UpdateEntityRequestDTO updateRequest = UpdateEntityRequestDTO.builder()
                .code("NEW-CODE")
                .description("New Description")
                .build();

        // When / Then - deve lanciare ResourceNotFoundException
        assertThatThrownBy(() -> updateEntityUseCase.execute(nonExistentId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Entity not found with id: " + nonExistentId);
    }

    @Test
    void execute_shouldTrimFields_whenUpdating() {
        // Given - creo un'entità
        EntityDTO created = fixtures.createEntity("TRIM-001", "Original");

        // When - aggiorno con spazi nei campi
        UpdateEntityRequestDTO updateRequest = UpdateEntityRequestDTO.builder()
                .code("  TRIM-001-UPD  ")
                .description("  Updated with spaces  ")
                .build();

        EntityDTO updated = updateEntityUseCase.execute(created.getId(), updateRequest);

        // Then - verifica che i campi siano stati normalizzati (trim)
        assertThat(updated.getCode()).isEqualTo("TRIM-001-UPD");
        assertThat(updated.getDescription()).isEqualTo("Updated with spaces");
    }

    @Test
    void execute_shouldUpdateOnlyCode_whenOnlyCodeChanged() {
        // Given
        EntityDTO created = fixtures.createEntity("CODE-001", "Original Description");
        String originalDescription = created.getDescription();

        // When - aggiorno solo il code
        UpdateEntityRequestDTO updateRequest = UpdateEntityRequestDTO.builder()
                .code("CODE-002")
                .description(originalDescription) // stessa description
                .build();

        EntityDTO updated = updateEntityUseCase.execute(created.getId(), updateRequest);

        // Then
        assertThat(updated.getCode()).isEqualTo("CODE-002");
        assertThat(updated.getDescription()).isEqualTo(originalDescription);
        assertThat(updated.getLastUpdateDate()).isNotNull();
    }

    @Test
    void execute_shouldUpdateOnlyDescription_whenOnlyDescriptionChanged() {
        // Given
        EntityDTO created = fixtures.createEntity("DESC-001", "Original Description");
        String originalCode = created.getCode();

        // When - aggiorno solo la description
        UpdateEntityRequestDTO updateRequest = UpdateEntityRequestDTO.builder()
                .code(originalCode) // stesso code
                .description("New Description")
                .build();

        EntityDTO updated = updateEntityUseCase.execute(created.getId(), updateRequest);

        // Then
        assertThat(updated.getCode()).isEqualTo(originalCode);
        assertThat(updated.getDescription()).isEqualTo("New Description");
        assertThat(updated.getLastUpdateDate()).isNotNull();
    }

    @Test
    void execute_shouldPreserveCreateFields_whenUpdating() {
        // Given
        EntityDTO created = fixtures.createEntity("AUDIT-001", "Original");

        // When - aggiorno l'entità
        UpdateEntityRequestDTO updateRequest = UpdateEntityRequestDTO.builder()
                .code("AUDIT-002")
                .description("Updated")
                .build();

        EntityDTO updated = updateEntityUseCase.execute(created.getId(), updateRequest);

        // Then - verifica che i campi di creazione non siano cambiati
        assertThat(updated.getCreateDate()).isEqualTo(created.getCreateDate());
        assertThat(updated.getCreateUser()).isEqualTo(created.getCreateUser());

        // Verifica che i campi di update siano popolati
        assertThat(updated.getLastUpdateDate()).isNotNull();
        assertThat(updated.getLastUpdateDate()).isAfter(updated.getCreateDate());
        assertThat(updated.getLastUpdateUser()).isNotNull();
    }

    @Test
    void execute_shouldUpdateMultipleTimes_whenCalledMultipleTimes() {
        // Given
        EntityDTO created = fixtures.createEntity("MULTI-001", "First");

        // When - primo update
        UpdateEntityRequestDTO firstUpdate = UpdateEntityRequestDTO.builder()
                .code("MULTI-002")
                .description("Second")
                .build();
        EntityDTO firstUpdated = updateEntityUseCase.execute(created.getId(), firstUpdate);

        // When - secondo update
        UpdateEntityRequestDTO secondUpdate = UpdateEntityRequestDTO.builder()
                .code("MULTI-003")
                .description("Third")
                .build();
        EntityDTO secondUpdated = updateEntityUseCase.execute(created.getId(), secondUpdate);

        // Then - verifica gli aggiornamenti
        assertThat(secondUpdated.getCode()).isEqualTo("MULTI-003");
        assertThat(secondUpdated.getDescription()).isEqualTo("Third");
        assertThat(secondUpdated.getLastUpdateDate()).isAfter(firstUpdated.getLastUpdateDate());

        // Verifica persistenza
        EntityDTO retrieved = getEntityUseCase.execute(created.getId());
        assertThat(retrieved.getCode()).isEqualTo("MULTI-003");
    }
}

