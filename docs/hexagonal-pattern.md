# Hexagonal Architecture - Ports Pattern

## Overview
This document defines the standard pattern for implementing IN ports and OUT ports
following hexagonal architecture principles.

## Core Principle
**The domain must never depend on infrastructure or HTTP frameworks.**

This is achieved through two port types:
- **IN ports** (`domain.port.in`): interfaces implemented by REST adapters — they bring data *into* the domain.
- **OUT ports** (`domain.port.out`): interfaces implemented by infrastructure adapters — they let the domain *reach out* to persistence, storage, or external services.

This ensures:
- ✅ Clear separation between HTTP layer, business logic, and infrastructure
- ✅ Testability (ports can be mocked independently)
- ✅ Adherence to hexagonal architecture
- ✅ Contract-first approach

---

## IN Ports

### 1. Define IN Port Interface
One interface per business operation in `domain.port.in`:

```java
package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.CreateEntityRequestDTO;
import it.quix.nomecliente.domain.ddd.dto.EntityDTO;
import org.springframework.http.ResponseEntity;

/**
 * IN port for creating a generic entity.
 */
public interface CreateEntityIn {

    /**
     * Create a new entity.
     *
     * @param request the entity creation request
     * @return HTTP 201 with the created entity
     */
    ResponseEntity<EntityDTO> createEntity(CreateEntityRequestDTO request);
}
```

### 2. REST Adapter Implements IN Port

```java
package it.quix.nomecliente.application;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
public class EntityRestAdapter implements CreateEntityIn, GetEntityIn, ListEntitiesIn {

    private final CreateEntityUseCase createEntityUseCase;

    @PostMapping
    @Override  // ← MANDATORY: implements IN port
    @Operation(summary = "Create a new entity")
    public ResponseEntity<EntityDTO> createEntity(
        @Valid @RequestBody CreateEntityRequestDTO request
    ) {
        EntityDTO created = createEntityUseCase.execute(request);
        return ResponseEntity
            .created(URI.create("/api/v1/entities/" + created.getId()))
            .body(created);
    }
}
```

### 3. Use Case Executes Business Logic

```java
package it.quix.nomecliente.domain.usecase;

@Service
@RequiredArgsConstructor
public class CreateEntityUseCaseImpl implements CreateEntityUseCase {

    private final EntityRepositoryOut entityRepository;   // ← OUT port injected
    private final SecurityContextHelper securityContextHelper;

    @Override
    public EntityDTO execute(CreateEntityRequestDTO request) {
        Entity entity = Entity.builder()
            .id(UUID.randomUUID())
            .code(request.getCode().trim())
            .description(request.getDescription().trim())
            .createDate(Instant.now())
            .createUser(securityContextHelper.getCurrentUsername())
            .canceled(false)
            .build();

        Entity saved = entityRepository.save(entity);   // ← calls OUT port
        return mapToDto(saved);
    }
}
```

### IN Port Best Practices

#### ✅ DO

1. **Match signatures exactly**: IN port method signature MUST match the REST adapter method.
   ```java
   // IN Port
   ResponseEntity<EntityDTO> createEntity(CreateEntityRequestDTO request);

   // REST Adapter — SAME signature
   @PostMapping
   @Override
   public ResponseEntity<EntityDTO> createEntity(@Valid @RequestBody CreateEntityRequestDTO request)
   ```

2. **Use `@Override`**: Always annotate REST methods with `@Override`.
   ```java
   @GetMapping("/{id}")
   @Override  // ← confirms implementation of IN port
   public EntityDTO getEntity(@PathVariable UUID id)
   ```

3. **HTTP status via return type**:
   ```java
   ResponseEntity<EntityDTO> createEntity(...);  // HTTP 201 Created
   EntityDTO getEntity(UUID id);                 // HTTP 200 OK
   void deleteEntity(UUID id);                   // HTTP 204 No Content
   ```

4. **REST parameters in ports — use primitives, not framework types**:
   ```java
   // ✅ GOOD
   Page<EntityDTO> listEntities(int page, int size, String sortBy, String sortDirection);

   // ❌ BAD — Spring Pageable in port
   Page<EntityDTO> listEntities(Pageable pageable);
   ```
   The REST adapter converts `@RequestParam` → `Pageable` internally:
   ```java
   @Override
   public Page<EntityDTO> listEntities(int page, int size, String sortBy, String sortDirection) {
       Pageable pageable = PageRequest.of(page, size,
           Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
       return listEntitiesUseCase.execute(pageable);
   }
   ```

5. **One port per operation**:
   ```java
   // ✅ GOOD
   CreateEntityIn / GetEntityIn / UpdateEntityIn / DeleteEntityIn / ListEntitiesIn

   // ❌ BAD
   EntityPortIn  // with all methods bundled together
   ```

#### ❌ DON'T

1. **Don't skip IN ports** — REST adapters must always implement ports.
2. **Don't put business logic in the REST adapter** — delegate everything to the use case.
3. **Don't add Spring / HTTP annotations to port interfaces** — they must be technology-agnostic.

---

## OUT Ports

### 1. Define OUT Port Interface
One interface per outbound dependency in `domain.port.out`.
The interface uses only **domain objects** (`domain.ddd.entity`, `domain.ddd.dto`) — never JPA entities or framework types.

```java
package it.quix.nomecliente.domain.port.out;

import it.quix.nomecliente.domain.ddd.entity.Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * OUT port for generic entity persistence operations.
 */
public interface EntityRepositoryOut {

    /**
     * Persist a new entity.
     *
     * @param entity the domain entity to save
     * @return the saved entity (with any DB-generated values populated)
     */
    Entity save(Entity entity);

    /**
     * Find entity by ID.
     *
     * @param id the entity ID
     * @return optional containing the entity, or empty if not found
     */
    Optional<Entity> findById(UUID id);

    /**
     * Find all entities with pagination and sorting.
     *
     * @param pageable pagination and sort parameters
     * @return page of domain entities
     */
    Page<Entity> findAll(Pageable pageable);

    /**
     * Update an existing entity.
     *
     * @param entity the domain entity with updated values
     * @return the updated entity
     */
    Entity update(Entity entity);

    /**
     * Check whether an entity with the given ID exists.
     *
     * @param id the entity ID
     * @return {@code true} if the entity exists
     */
    boolean existsById(UUID id);
}
```

### 2. Infrastructure Adapter Implements OUT Port

Located in `infrastructure.persistence.jpa.adapter`, annotated with `@Component`.
It maps between the **domain model** (`domain.ddd.entity.Entity`) and the **JPA entity** (`infrastructure.persistence.jpa.entity.EntityJpaEntity`).

```java
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
 * Translates between the domain model and the JPA persistence model.
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
        return jpaRepository.findAll(pageable)
            .map(this::mapToDomain);
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

    // ── Private mapping methods ──────────────────────────────────────────────

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

    private Entity mapToDomain(EntityJpaEntity jpa) {
        return Entity.builder()
            .id(jpa.getId())
            .code(jpa.getCode())
            .description(jpa.getDescription())
            .createDate(jpa.getCreateDate())
            .createUser(jpa.getCreateUser())
            .lastUpdateDate(jpa.getLastUpdateDate())
            .lastUpdateUser(jpa.getLastUpdateUser())
            .canceled(jpa.getCanceled())
            .build();
    }
}
```

### 3. Spring Data JPA Repository (internal to infrastructure)

The `EntityJpaRepository` is a Spring Data interface used **only** by the adapter.
It never crosses the boundary into the domain.

```java
package it.quix.nomecliente.infrastructure.persistence.jpa.repository;

import it.quix.nomecliente.infrastructure.persistence.jpa.entity.EntityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EntityJpaRepository extends JpaRepository<EntityJpaEntity, UUID> {
    // Custom query methods added here as needed
}
```

### OUT Port Best Practices

#### ✅ DO

1. **Use domain objects in method signatures** — never expose JPA entities outside the adapter.
   ```java
   // ✅ GOOD — domain entity crosses the port boundary
   Entity save(Entity entity);

   // ❌ BAD — JPA entity leaks into the domain
   EntityJpaEntity save(EntityJpaEntity jpaEntity);
   ```

2. **Keep mapping private inside the adapter** — `mapToJpaEntity` / `mapToDomain` are private methods, not exposed.

3. **Name OUT ports after their role**, not the technology:
   ```java
   // ✅ GOOD
   EntityRepositoryOut      // persistence role
   FileStorageOut           // storage role
   NotificationServiceOut   // external service role

   // ❌ BAD
   EntityJpaRepository      // leaks the technology
   S3BucketAdapter          // leaks the provider
   ```

4. **One OUT port per external dependency** (DB table group, storage bucket, external API):
   ```java
   // ✅ GOOD
   EntityRepositoryOut   // entities table
   AuditLogOut           // audit_logs table (separate concern)

   // ❌ BAD
   DatabaseOut           // everything in one port
   ```

5. **Annotate the implementation with `@Component`**, not `@Service` (it is infrastructure, not business logic).

#### ❌ DON'T

1. **Don't inject `EntityJpaRepository` directly into use cases** — always go through the OUT port.
   ```java
   // ❌ BAD — use case depends on infrastructure
   @Service
   public class CreateEntityUseCaseImpl {
       private final EntityJpaRepository jpaRepository;  // ← wrong!
   }

   // ✅ GOOD — use case depends on the domain port
   @Service
   public class CreateEntityUseCaseImpl {
       private final EntityRepositoryOut entityRepository;  // ← correct
   }
   ```

2. **Don't put business logic in the adapter** — the adapter only translates and delegates.
   ```java
   // ❌ BAD — business logic in adapter
   @Override
   public Entity save(Entity entity) {
       if (entity.getCode() == null) throw new BusinessException(...); // ← NO
       ...
   }
   ```

3. **Don't return `Optional` from adapters when the domain always expects a value** — let the use case decide how to handle absence.

---

## Flow Diagram

```
HTTP Request
     │
     ▼
[REST Adapter]  ──implements──▶  [IN Port]         (application layer)
     │
     │ delegates to
     ▼
[Use Case Impl] ──implements──▶  [Use Case]        (domain layer)
     │
     │ calls
     ▼
[OUT Port]      ◀──implements──  [Infra Adapter]   (infrastructure layer)
     │
     ▼
Database / External Service
```

---

## Checklist

When implementing a new feature:

**IN Port side**
- [ ] Define IN port interface in `domain.port.in` (one per operation)
- [ ] REST adapter `implements` all IN ports of the feature
- [ ] All public REST methods annotated with `@Override`
- [ ] Method signatures match exactly (parameters, return type)
- [ ] REST adapter is thin: validation + delegation only, no business logic
- [ ] No `try/catch` in REST adapter (errors handled by `GlobalExceptionHandler`)

**Domain / Use Case**
- [ ] Use case interface defined in `domain.usecase`
- [ ] Use case implementation (`*Impl`) annotated with `@Service` and contains all business logic
- [ ] Use case depends only on OUT port interfaces, not on infrastructure classes

**OUT Port side**
- [ ] OUT port interface defined in `domain.port.out` with domain-object signatures
- [ ] Infrastructure adapter in `infrastructure.persistence.jpa.adapter` annotated with `@Component`
- [ ] Adapter maps between domain model and JPA entity in private methods
- [ ] `EntityJpaRepository` (Spring Data) is used **only** inside the adapter, never injected elsewhere

---

## Example: Complete CRUD

```java
// ── 1. IN Ports ──────────────────────────────────────────────────────────────

public interface CreateEntityIn {
    ResponseEntity<EntityDTO> createEntity(CreateEntityRequestDTO request);
}
public interface GetEntityIn {
    EntityDTO getEntity(UUID id);
}
public interface ListEntitiesIn {
    Page<EntityDTO> listEntities(int page, int size, String sortBy, String sortDirection);
}
public interface UpdateEntityIn {
    EntityDTO updateEntity(UUID id, UpdateEntityRequestDTO request);
}
public interface DeleteEntityIn {
    void deleteEntity(UUID id);
}

// ── 2. REST Adapter ───────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
public class EntityRestAdapter implements
        CreateEntityIn, GetEntityIn, ListEntitiesIn, UpdateEntityIn, DeleteEntityIn {

    private final CreateEntityUseCase createEntityUseCase;
    private final GetEntityUseCase    getEntityUseCase;
    private final ListEntitiesUseCase listEntitiesUseCase;
    private final UpdateEntityUseCase updateEntityUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    @PostMapping   @Override
    public ResponseEntity<EntityDTO> createEntity(@Valid @RequestBody CreateEntityRequestDTO request) {
        EntityDTO created = createEntityUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/entities/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")   @Override
    public EntityDTO getEntity(@PathVariable UUID id) {
        return getEntityUseCase.execute(id);
    }

    @GetMapping   @Override
    public Page<EntityDTO> listEntities(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDirection) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return listEntitiesUseCase.execute(pageable);
    }

    @PutMapping("/{id}")   @Override
    public EntityDTO updateEntity(@PathVariable UUID id,
                                  @Valid @RequestBody UpdateEntityRequestDTO request) {
        return updateEntityUseCase.execute(id, request);
    }

    @DeleteMapping("/{id}")   @Override
    public void deleteEntity(@PathVariable UUID id) {
        deleteEntityUseCase.execute(id);
    }
}

// ── 3. OUT Port ───────────────────────────────────────────────────────────────

public interface EntityRepositoryOut {
    Entity save(Entity entity);
    Optional<Entity> findById(UUID id);
    Page<Entity> findAll(Pageable pageable);
    Entity update(Entity entity);
    boolean existsById(UUID id);
}

// ── 4. Infrastructure Adapter ─────────────────────────────────────────────────

@Component
@RequiredArgsConstructor
public class EntityRepositoryAdapter implements EntityRepositoryOut {

    private final EntityJpaRepository jpaRepository;

    @Override public Entity save(Entity e)                     { return mapToDomain(jpaRepository.save(mapToJpa(e))); }
    @Override public Optional<Entity> findById(UUID id)        { return jpaRepository.findById(id).map(this::mapToDomain); }
    @Override public Page<Entity> findAll(Pageable p)          { return jpaRepository.findAll(p).map(this::mapToDomain); }
    @Override public Entity update(Entity e)                   { return mapToDomain(jpaRepository.save(mapToJpa(e))); }
    @Override public boolean existsById(UUID id)               { return jpaRepository.existsById(id); }

    private EntityJpaEntity mapToJpa(Entity e) { /* field-by-field mapping */ }
    private Entity mapToDomain(EntityJpaEntity j) { /* field-by-field mapping */ }
}
```

