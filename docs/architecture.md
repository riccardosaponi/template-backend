# Architecture

## Goals
- Standardized backend skeleton for rapid project bootstrapping
- Contract-first APIs (OpenAPI)
- Deterministic schema evolution (Liquibase)
- Deterministic automated tests (Testcontainers)

---

## Core principle
**The domain must never depend on infrastructure or HTTP frameworks.**

```
HTTP Request
     │
     ▼
[*RestAdapter]  ──implements──▶  [IN Port]         (application layer)
     │ delegates to
     ▼
[*UseCaseImpl]  ──implements──▶  [UseCase]         (domain layer)
     │ calls
     ▼
[OUT Port]      ◀──implements──  [Infra Adapter]   (infrastructure layer)
     │
     ▼
Database / External Service
```

---

## Repository layout (multi-module Maven)

```
<repo-root>/
  pom.xml                              # Parent POM (packaging=pom)

  apps/
    backoffice/                        # Spring Boot runnable module
      src/main/java/${BASE_PACKAGE}/
        application/                   # REST adapters (*RestAdapter, implement IN ports)
        config/                        # Spring @Configuration, explicit bean wiring
        config/security/               # SecurityConfig (SecurityFilterChain)
      src/main/resources/
        application.yml
        db/changelog/                  # Liquibase master + changesets

  libs/
    shared-core/                       # Cross-cutting concerns
      src/main/java/${BASE_PACKAGE}/
        config/                        # WebConfig (CORS), GlobalExceptionHandler
        config/security/               # JWT classes, SecurityContextHelper
        domain/exception/              # Shared runtime exceptions
        domain/ddd/dto/error/          # Error response DTOs

    {domain}-domain/                   # One module per feature area
      src/main/java/${BASE_PACKAGE}/
        domain/port/in/                # IN ports (use case interfaces)
        domain/port/out/               # OUT ports (repository/client interfaces)
        domain/usecase/                # UseCase interface + *UseCaseImpl (business logic)
        domain/ddd/dto/                # Request/Response DTOs
        domain/ddd/entity/             # Domain entities
        domain/ddd/enumeration/        # Enumerations
        infrastructure/                # OUT port implementations (JDBC, HTTP clients, storage)
```

> Maven module naming: `{project}-shared-core`, `{project}-{domain}-domain`, `{project}-backoffice`.

Contract: `api/openapi.yaml`
DB migrations: `apps/backoffice/src/main/resources/db/changelog/db.changelog-master.yaml`

---

## Layer rules

| Layer | Module / Package | Rule |
|---|---|---|
| `*RestAdapter` | `apps/backoffice/.../application/` | Thin: HTTP mapping + `@Valid` + single use-case call + build `ResponseEntity`. No business logic. No `try/catch`. |
| `domain.port.in` | `libs/{domain}-domain/.../domain/port/in/` | One interface per operation. Method signature = REST adapter public method. No Spring/HTTP types in signature. |
| `*UseCaseImpl` | `libs/{domain}-domain/.../domain/usecase/` | `@Service`. All business rules here. Depends only on OUT port interfaces, never on infrastructure classes. |
| `domain.port.out` | `libs/{domain}-domain/.../domain/port/out/` | Interface only. Domain-object signatures (`domain.ddd.*`). Never exposes infrastructure types. |
| `infrastructure.*` | `libs/{domain}-domain/.../infrastructure/` | `@Component`. Implements OUT ports. Only layer aware of DB/HTTP/storage. Mapping logic is private. |
| Shared utils | `libs/shared-core/.../config/` | WebConfig, GlobalExceptionHandler, JWT helpers. No business logic. |
| `config/` | `apps/backoffice/.../config/` | `@Configuration` and `SecurityFilterChain`. No business logic. |

---

## IN Ports

One interface per business operation in `domain/port/in/`:

```java
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
```

Rules:
- Method signature **must match** the REST adapter public method exactly (parameters + return type).
- Return `ResponseEntity<T>` only for HTTP 201 (POST create). Use plain `T` or `void` for other operations.
- Pagination: use primitives `(int page, int size, String sortBy, String sortDirection)` — never `Pageable`.
- Never add Spring or HTTP annotations to port interfaces.

The REST adapter implements all IN ports of the feature:

```java
// application/EntityRestAdapter.java
@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
public class EntityRestAdapter implements CreateEntityIn, GetEntityIn, ListEntitiesIn, UpdateEntityIn, DeleteEntityIn {

    private final CreateEntityUseCase createEntityUseCase;
    private final GetEntityUseCase    getEntityUseCase;
    private final ListEntitiesUseCase listEntitiesUseCase;
    private final UpdateEntityUseCase updateEntityUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    @PostMapping
    @Override  // ← mandatory on every REST method
    public ResponseEntity<EntityDTO> createEntity(@Valid @RequestBody CreateEntityRequestDTO request) {
        EntityDTO created = createEntityUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/entities/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    @Override
    public EntityDTO getEntity(@PathVariable UUID id) {
        return getEntityUseCase.execute(id);
    }

    @GetMapping
    @Override
    public Page<EntityDTO> listEntities(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDirection) {
        // Pageable is built here, in the adapter — never passed through the IN port
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return listEntitiesUseCase.execute(pageable);
    }

    @PutMapping("/{id}")
    @Override
    public EntityDTO updateEntity(@PathVariable UUID id, @Valid @RequestBody UpdateEntityRequestDTO request) {
        return updateEntityUseCase.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @Override
    public void deleteEntity(@PathVariable UUID id) {
        deleteEntityUseCase.execute(id);
    }
}
```

---

## Use Cases

```java
// domain/usecase/CreateEntityUseCase.java
public interface CreateEntityUseCase {
    EntityDTO execute(CreateEntityRequestDTO request);
}

// domain/usecase/CreateEntityUseCaseImpl.java
@Service
@RequiredArgsConstructor
public class CreateEntityUseCaseImpl implements CreateEntityUseCase {

    private final EntityRepositoryOut entityRepository; // ← OUT port, never a JDBC/infra class

    @Override
    public EntityDTO execute(CreateEntityRequestDTO request) {
        // All validation and business rules belong here
        Entity entity = Entity.builder()
            .id(UUID.randomUUID().toString())
            .code(request.getCode().trim())
            .description(request.getDescription().trim())
            .createdBy(securityContextHelper.getCurrentUsername())
            .createdDate(Instant.now())
            .canceled(0)
            .build();
        return mapToDto(entityRepository.save(entity));
    }
}
```

---

## OUT Ports

One interface per outbound dependency in `domain/port/out/`. Signatures use only domain objects.

```java
// domain/port/out/EntityRepositoryOut.java
public interface EntityRepositoryOut {
    Entity save(Entity entity);
    Optional<Entity> findById(String id);
    Page<Entity> findAll(Pageable pageable);
    Entity update(Entity entity);
    boolean existsById(String id);
}
```

Rules:
- Name after role, not technology: `EntityRepositoryOut`, `FileStorageOut`, `NotificationServiceOut`.
- One OUT port per external dependency (DB table group, storage bucket, external API).
- Never inject `NamedParameterJdbcTemplate` or any infrastructure class directly into use cases.
- No business logic in the adapter — pure translation and delegation only.

Infrastructure adapter (JDBC — never JPA/Hibernate):

```java
// infrastructure/persistence/EntityRepositoryAdapter.java
@Component
@RequiredArgsConstructor
public class EntityRepositoryAdapter implements EntityRepositoryOut {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Entity save(Entity entity) {
        String sql = """
            INSERT INTO entity (entity_id, code, description, created_by, created_date, canceled)
            VALUES (:id, :code, :description, :createdBy, :createdDate, 0)
            """;
        jdbc.update(sql, new MapSqlParameterSource()
            .addValue("id",          entity.getId())
            .addValue("code",        entity.getCode())
            .addValue("description", entity.getDescription())
            .addValue("createdBy",   entity.getCreatedBy())
            .addValue("createdDate", entity.getCreatedDate()));
        return entity;
    }

    @Override
    public Optional<Entity> findById(String id) {
        String sql = """
            SELECT * FROM entity WHERE entity_id = :id AND canceled = 0
            """;
        List<Entity> result = jdbc.query(sql,
            new MapSqlParameterSource("id", id),
            (rs, row) -> mapRow(rs));
        return result.stream().findFirst();
    }

    // Mapping is always private — domain model never leaks JDBC internals
    private Entity mapRow(ResultSet rs) throws SQLException {
        return Entity.builder()
            .id(rs.getString("entity_id"))
            .code(rs.getString("code"))
            .description(rs.getString("description"))
            .createdBy(rs.getString("created_by"))
            .createdDate(rs.getObject("created_date", Instant.class))
            .canceled(rs.getInt("canceled"))
            .build();
    }
}
```

---

## Feature implementation checklist

**IN Port side**
- [ ] One IN port interface per operation in `domain.port.in`
- [ ] `*RestAdapter implements` all IN ports of the feature
- [ ] Every public REST method annotated with `@Override`
- [ ] Method signatures match exactly (parameters + return type)
- [ ] No business logic and no `try/catch` in the adapter

**Use Case**
- [ ] Interface + `@Service` impl in `domain.usecase`
- [ ] All business logic in `*UseCaseImpl`
- [ ] Depends only on OUT port interfaces — never on infrastructure classes

**OUT Port side**
- [ ] Interface in `domain.port.out` with domain-object signatures
- [ ] `@Component` adapter in `infrastructure.persistence`
- [ ] `NamedParameterJdbcTemplate` with SQL text blocks — never JPA/Hibernate
- [ ] Mapping logic is private inside the adapter

