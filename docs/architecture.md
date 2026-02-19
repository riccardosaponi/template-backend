# Architecture

## Goals
- Standardized backend skeleton for rapid project bootstrapping
- Contract-first APIs (OpenAPI)
- Deterministic schema evolution (Liquibase)
- Deterministic automated tests (integration with Testcontainers)

## Hexagonal architecture

### Driving adapter (REST)
**Package**: `it.quix.nomecliente.application`
- REST adapters (controllers), typically named `*RestAdapter`.
- **MUST implement IN ports** from `it.quix.nomecliente.domain.port.in`.
- Should stay thin: HTTP mapping/validation + delegation to use cases.
- All public REST methods annotated with `@PostMapping`, `@GetMapping`, etc. should have `@Override` to implement IN ports.

**Example**:
```java
@RestController
@RequestMapping("/api/v1/entities")
public class EntityRestAdapter implements CreateEntityIn, GetEntityIn, ... {
    
    private final CreateEntityUseCase createEntityUseCase;
    
    @PostMapping
    @Override  // implements CreateEntityIn
    public ResponseEntity<EntityDto> createEntity(@Valid @RequestBody CreateEntityRequest request) {
        EntityDto created = createEntityUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/entities/" + created.getId())).body(created);
    }
}
```

### Domain (hexagon)
**Package**: `it.quix.nomecliente.domain`

- **IN ports** (`domain.port.in`): 
  - Interfaces defining the **REST API contract**.
  - One interface per operation (e.g., `CreateEntityIn`, `GetEntityIn`).
  - Method signatures **MUST match** the REST adapter public methods (same parameters, same return type).
  - For operations returning HTTP 201 Created, use `ResponseEntity<T>` as return type.
  - For pagination, use individual parameters (`int page, int size, String sortBy, String sortDirection`) not `Pageable`.
  
- **Use cases** (`domain.usecase`):
  - interface: contract used by the REST adapter (e.g., `CreateEntityUseCase`)
  - implementation: `InterfaceNameImpl` (contains **all business logic**)
  - Use cases are called by REST adapters to execute business operations.
  - Use cases work with domain objects and call OUT ports for persistence/external services.
  
- **OUT ports** (`domain.port.out`): interfaces for outbound dependencies (DB, storage, external HTTP clients).
- **Domain model** (`domain.ddd`): DTO / entity / enumeration.

### Driven adapters (infrastructure)
**Package**: `it.quix.nomecliente.infrastructure`
- Implementations of `domain.port.out`.
- Examples:
  - JPA repositories / adapters
  - `JdbcTemplate` repositories (plain SQL)
  - REST clients to third-party services
  - filesystem/object storage adapters

### Configuration
**Package**: `it.quix.nomecliente.config`
- Spring configuration / wiring when needed.

## Contract
OpenAPI is under `api/openapi.yaml` and acts as the primary contract.

## Database migrations
Liquibase master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
