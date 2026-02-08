package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import it.quix.nomecliente.fixtures.EntityTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ListEntitiesUseCase.
 * Tests entity listing with pagination and sorting.
 */
class ListEntitiesUseCaseIT extends BaseUseCaseIT {

    @Autowired
    private ListEntitiesUseCase listEntitiesUseCase;

    @Autowired
    private EntityTestFixtures fixtures;

    @Test
    void execute_shouldReturnEmptyPage_whenNoEntitiesExist() {
        // Given - nessuna entità nel database (il database è pulito ad ogni test)
        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<EntityDTO> result = listEntitiesUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    void execute_shouldReturnAllEntities_whenEntitiesExist() {
        // Given - creo 3 entità
        fixtures.createEntity("LIST-001", "First Entity");
        fixtures.createEntity("LIST-002", "Second Entity");
        fixtures.createEntity("LIST-003", "Third Entity");

        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<EntityDTO> result = listEntitiesUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void execute_shouldRespectPagination_whenMultiplePagesExist() {
        // Given - creo 5 entità
        for (int i = 1; i <= 5; i++) {
            fixtures.createEntity("PAGE-" + String.format("%03d", i), "Entity " + i);
        }

        // When - richiedo pagina 0 con size 2
        Pageable pageable = PageRequest.of(0, 2);
        Page<EntityDTO> firstPage = listEntitiesUseCase.execute(pageable);

        // Then - prima pagina
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getNumber()).isZero();

        // When - richiedo pagina 1
        Pageable pageableSecond = PageRequest.of(1, 2);
        Page<EntityDTO> secondPage = listEntitiesUseCase.execute(pageableSecond);

        // Then - seconda pagina
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getNumber()).isEqualTo(1);

        // When - richiedo pagina 2 (ultima)
        Pageable pageableThird = PageRequest.of(2, 2);
        Page<EntityDTO> thirdPage = listEntitiesUseCase.execute(pageableThird);

        // Then - terza pagina con 1 elemento
        assertThat(thirdPage.getContent()).hasSize(1);
        assertThat(thirdPage.getNumber()).isEqualTo(2);
    }

    @Test
    void execute_shouldSortByCodeAscending_whenSortByCodeAsc() {
        // Given - creo entità con codici non in ordine
        fixtures.createEntity("SORT-003", "Third");
        fixtures.createEntity("SORT-001", "First");
        fixtures.createEntity("SORT-002", "Second");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "code"));

        // When
        Page<EntityDTO> result = listEntitiesUseCase.execute(pageable);

        // Then - verifica ordinamento ascendente per code
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("SORT-001");
        assertThat(result.getContent().get(1).getCode()).isEqualTo("SORT-002");
        assertThat(result.getContent().get(2).getCode()).isEqualTo("SORT-003");
    }

    @Test
    void execute_shouldSortByCodeDescending_whenSortByCodeDesc() {
        // Given - creo entità con codici non in ordine
        fixtures.createEntity("DESC-001", "First");
        fixtures.createEntity("DESC-003", "Third");
        fixtures.createEntity("DESC-002", "Second");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "code"));

        // When
        Page<EntityDTO> result = listEntitiesUseCase.execute(pageable);

        // Then - verifica ordinamento discendente per code
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("DESC-003");
        assertThat(result.getContent().get(1).getCode()).isEqualTo("DESC-002");
        assertThat(result.getContent().get(2).getCode()).isEqualTo("DESC-001");
    }

    @Test
    void execute_shouldSortByDescription_whenSortByDescription() {
        // Given - creo entità con description diverse
        fixtures.createEntity("DESC-A", "Zebra");
        fixtures.createEntity("DESC-B", "Apple");
        fixtures.createEntity("DESC-C", "Mango");

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "description"));

        // When
        Page<EntityDTO> result = listEntitiesUseCase.execute(pageable);

        // Then - verifica ordinamento per description
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Apple");
        assertThat(result.getContent().get(1).getDescription()).isEqualTo("Mango");
        assertThat(result.getContent().get(2).getDescription()).isEqualTo("Zebra");
    }

    @Test
    void execute_shouldIncludeAllFields_whenRetrievingEntities() {
        // Given
        EntityDTO created = fixtures.createEntity("FIELDS-001", "Complete Entity");

        Pageable pageable = PageRequest.of(0, 20);

        // When
        Page<EntityDTO> result = listEntitiesUseCase.execute(pageable);

        // Then - verifica che tutti i campi siano presenti
        assertThat(result.getContent()).hasSize(1);
        EntityDTO entity = result.getContent().get(0);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCode()).isEqualTo("FIELDS-001");
        assertThat(entity.getDescription()).isEqualTo("Complete Entity");
        assertThat(entity.getCreateDate()).isNotNull();
        assertThat(entity.getCreateUser()).isNotNull();
        assertThat(entity.getCanceled()).isNotNull();
    }
}

