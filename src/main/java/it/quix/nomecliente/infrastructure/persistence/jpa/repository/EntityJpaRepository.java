package it.quix.nomecliente.infrastructure.persistence.jpa.repository;

import it.quix.nomecliente.infrastructure.persistence.jpa.entity.EntityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for EntityJpaEntity.
 */
@Repository
public interface EntityJpaRepository extends JpaRepository<EntityJpaEntity, UUID> {
    // Additional query methods can be added here if needed
}

