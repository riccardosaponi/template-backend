package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.domain.exception.ResourceNotFoundException;
import it.quix.nomecliente.fixtures.EntityTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for DeleteEntityUseCase.
 * Tests logical deletion (canceled flag) with database persistence.
 */
class DeleteEntityUseCaseIT extends BaseUseCaseIT {

    @Autowired
    private DeleteEntityUseCase deleteEntityUseCase;

    @Autowired
    private GetEntityUseCase getEntityUseCase;

    @Autowired
    private EntityTestFixtures fixtures;

    @Test
    void execute_shouldSetCanceledToTrue_whenDeletingEntity() {
        // Given - creo un'entità
        EntityDTO created = fixtures.createEntity("DEL-001", "Entity to Delete");

        // Verifica che inizialmente canceled sia false
        assertThat(created.getCanceled()).isFalse();

        // When - eseguo il delete (logico)
        deleteEntityUseCase.execute(created.getId());

        // Then - verifica che l'entità sia ancora recuperabile ma canceled=true
        EntityDTO deleted = getEntityUseCase.execute(created.getId());
        assertThat(deleted).isNotNull();
        assertThat(deleted.getId()).isEqualTo(created.getId());
        assertThat(deleted.getCanceled()).isTrue();
    }

    @Test
    void execute_shouldThrowException_whenEntityNotFound() {
        // Given - un ID che non esiste
        UUID nonExistentId = UUID.randomUUID();

        // When / Then - deve lanciare ResourceNotFoundException
        assertThatThrownBy(() -> deleteEntityUseCase.execute(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Entity not found with id: " + nonExistentId);
    }

    @Test
    void execute_shouldUpdateAuditFields_whenDeleting() {
        // Given
        EntityDTO created = fixtures.createEntity("AUDIT-DEL-001", "Entity with Audit");

        // When - eseguo il delete
        deleteEntityUseCase.execute(created.getId());

        // Then - verifica che i campi audit siano aggiornati
        EntityDTO deleted = getEntityUseCase.execute(created.getId());

        assertThat(deleted.getLastUpdateDate()).isNotNull();
        assertThat(deleted.getLastUpdateDate()).isAfter(deleted.getCreateDate());
        assertThat(deleted.getLastUpdateUser()).isEqualTo("test-user");

        // I campi di creazione devono rimanere invariati
        assertThat(deleted.getCreateDate()).isEqualTo(created.getCreateDate());
        assertThat(deleted.getCreateUser()).isEqualTo(created.getCreateUser());
    }

    @Test
    void execute_shouldPreserveEntityData_whenDeleting() {
        // Given
        EntityDTO created = fixtures.createEntity("PRESERVE-001", "Preserved Entity");

        // When - eseguo il delete
        deleteEntityUseCase.execute(created.getId());

        // Then - verifica che code e description siano preservati
        EntityDTO deleted = getEntityUseCase.execute(created.getId());

        assertThat(deleted.getCode()).isEqualTo("PRESERVE-001");
        assertThat(deleted.getDescription()).isEqualTo("Preserved Entity");
        assertThat(deleted.getId()).isEqualTo(created.getId());
    }

    @Test
    void execute_shouldBeIdempotent_whenDeletingAlreadyDeletedEntity() {
        // Given - creo ed elimino un'entità
        EntityDTO created = fixtures.createEntity("IDEMPOTENT-001", "To Delete Twice");
        deleteEntityUseCase.execute(created.getId());

        EntityDTO firstDelete = getEntityUseCase.execute(created.getId());
        assertThat(firstDelete.getCanceled()).isTrue();

        // When - eseguo di nuovo il delete sulla stessa entità
        deleteEntityUseCase.execute(created.getId());

        // Then - l'entità rimane canceled=true senza errori
        EntityDTO secondDelete = getEntityUseCase.execute(created.getId());
        assertThat(secondDelete.getCanceled()).isTrue();

        // Verifica che l'entità esista ancora nel database
        fixtures.assertEntityExists(created.getId());
    }

    @Test
    void execute_shouldNotPhysicallyDelete_whenDeleting() {
        // Given - creo un'entità
        EntityDTO created = fixtures.createEntity("PHYSICAL-001", "Not Physically Deleted");
        UUID entityId = created.getId();

        // When - eseguo il delete logico
        deleteEntityUseCase.execute(entityId);

        // Then - l'entità deve ancora esistere nel database
        // (posso recuperarla per ID anche se canceled=true)
        EntityDTO deleted = getEntityUseCase.execute(entityId);

        assertThat(deleted).isNotNull();
        assertThat(deleted.getId()).isEqualTo(entityId);
        assertThat(deleted.getCanceled()).isTrue();

        // Verifica che esista fisicamente nel database
        fixtures.assertEntityExists(entityId);
    }

    @Test
    void execute_shouldAllowRecovery_afterLogicalDelete() {
        // Given - creo e cancello un'entità
        EntityDTO created = fixtures.createEntity("RECOVER-001", "Recoverable Entity");
        deleteEntityUseCase.execute(created.getId());

        EntityDTO deleted = getEntityUseCase.execute(created.getId());
        assertThat(deleted.getCanceled()).isTrue();

        // Then - l'entità può essere teoricamente "recuperata"
        // (questo test dimostra che il dato non è perso)
        assertThat(deleted.getCode()).isEqualTo("RECOVER-001");
        assertThat(deleted.getDescription()).isEqualTo("Recoverable Entity");

        // In un sistema reale, si potrebbe implementare un "undelete"
        // che setta canceled=false
    }
}

