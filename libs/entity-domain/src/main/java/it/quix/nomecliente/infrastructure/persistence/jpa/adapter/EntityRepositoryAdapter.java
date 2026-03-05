package it.quix.nomecliente.infrastructure.persistence.jpa.adapter;

import it.quix.nomecliente.domain.ddd.entity.Entity;
import it.quix.nomecliente.domain.port.out.EntityRepositoryOut;
import it.quix.nomecliente.infrastructure.persistence.jpa.entity.EntityJpaEntity;
import it.quix.nomecliente.infrastructure.persistence.jpa.repository.EntityJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing EntityRepositoryOut.
 */
@Component
@RequiredArgsConstructor
public class EntityRepositoryAdapter implements EntityRepositoryOut {

    private final EntityJpaRepository jpaRepository;

    @Override
    public Entity save(Entity entity) {
        EntityJpaEntity jpaEntity = mapToJpaEntity(entity);
        EntityJpaEntity saved = jpaRepository.save(jpaEntity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Entity> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(this::mapToDomain);
    }

    @Override
    public Page<Entity> findAll(Pageable pageable) {
        Page<EntityJpaEntity> entities = jpaRepository.findAll(pageable);
        return entities.map(this::mapToDomain);
    }

    @Override
    public Entity update(Entity entity) {
        EntityJpaEntity jpaEntity = mapToJpaEntity(entity);
        EntityJpaEntity updated = jpaRepository.save(jpaEntity);
        return mapToDomain(updated);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    private EntityJpaEntity mapToJpaEntity(Entity entity) {
        return EntityJpaEntity.builder()
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

    private Entity mapToDomain(EntityJpaEntity jpaEntity) {
        return Entity.builder()
            .id(jpaEntity.getId())
            .code(jpaEntity.getCode())
            .description(jpaEntity.getDescription())
            .createDate(jpaEntity.getCreateDate())
            .createUser(jpaEntity.getCreateUser())
            .lastUpdateDate(jpaEntity.getLastUpdateDate())
            .lastUpdateUser(jpaEntity.getLastUpdateUser())
            .canceled(jpaEntity.getCanceled())
            .build();
    }
}

