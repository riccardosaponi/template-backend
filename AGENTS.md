# AGENTS.md

## Purpose
Spring Boot backend template using PostgreSQL + Liquibase.
Designed for deterministic development and testing (CI-ready), including spec-driven workflows.

Architecture: **Hexagonal (Ports & Adapters)**.

## Project coordinates
- **GroupId**: `it.quix`
- **ArtifactId**: `nomecliente-backend-core`
- **Base package**: `it.quix.nomecliente`

## Runtime configuration
This template uses a **single** `src/main/resources/application.yml`.
All runtime settings are controlled via environment variables with defaults.

## Quick commands
- Tests + coverage check (PostgreSQL via Testcontainers): `./mvnw -q verify`
- Run locally with docker-compose Postgres: `./mvnw spring-boot:run`

## Hexagonal architecture map (repo conventions)

### Driving adapter: REST (`it.quix.nomecliente.application`)
- REST controllers / adapters (e.g. `*RestAdapter`).
- They implement **IN ports** from `it.quix.nomecliente.domain.port.in`.
- They must be thin: HTTP mapping + validation + delegation.
- They delegate to the domain **use case implementation** classes (`*Impl`).

### Hexagon: Domain (`it.quix.nomecliente.domain`)

#### `domain.port.in`
- **IN ports**: pure interfaces that define the API contract.
- Used by REST adapters to define endpoints.

#### `domain.usecase`
- **Use case interface**: contract used by the REST adapter.
- **Use case implementation**: `InterfaceNameImpl`.
- Implementations contain the business logic.

> Rule of thumb: the REST adapter should not contain business rules; those belong in `*Impl`.

#### `domain.port.out`
- **OUT ports**: interfaces for outbound operations.
- Examples:
  - persistence repositories (JPA/JdbcTemplate/etc.)
  - filesystem/object storage
  - REST clients to third-party services

#### `domain.ddd`
- Domain model (DTO / entity / enumeration).

### Driven adapters: Infrastructure (`it.quix.nomecliente.infrastructure`)
- All implementations of `domain.port.out` live here.
- Examples:
  - JPA adapter (Spring Data)
  - JdbcTemplate adapter (plain SQL)
  - REST client adapter
  - filesystem storage adapter

### Configuration (`it.quix.nomecliente.config`)
- Spring configuration and explicit bean wiring when required.

## Liquibase
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Changesets: `src/main/resources/db/changelog/changes/**`

## Language 
All code must be in English (identifiers, comments, docs).

## Test strategy (mandatory)

### Integration tests
- Use Spring Boot context + PostgreSQL via Testcontainers.
- Liquibase must apply schema from scratch.
- Use MockMvc to test endpoints.
- All use cases must be covered (success path, validation, authorization, domain edge cases).

### External systems
- Do not call live systems in CI.
- Prefer stubs/mocks or WireMock for HTTP integrations.

## Spec-driven workflow
- Feature specs (API + business logic + persistence): `spec/features/**`
- Contract: `api/openapi.yaml` (source of truth for endpoints and payloads)
- Prompt blueprints: `docs/prompts/**` (ready-to-use templates for common patterns)

## Required scenarios (baseline for each new endpoint)
- Success path
- Validation -> 400
- Authorization -> 403 (once security is enabled)
- Domain edge case (conflicts, inheritance/override rules, etc.)

## Implementation approaches

### Approach 1: Quick patterns (recommended for common cases)
Use ready-made templates from **`docs/prompts/quick-prompts.md`**.

Choose from 7 standard patterns:
1. CRUD Entity with database
2. Search/Filter API
3. File Upload with metadata
4. Aggregation/Statistics
5. Batch Processing
6. Entity Relationship Management
7. State Machine/Workflow

**Time**: 2-5 minutes to customize and use.

See `docs/prompts/README.md` for detailed usage instructions.

### Approach 2: Custom feature (for complex scenarios)
Use the comprehensive template from **`docs/prompts/rest-api-blueprint.md`**.

Includes:
- Business requirements and rules
- API contract specification
- Data model and schema design
- Acceptance criteria
- Testing requirements
- Implementation checklist

**Time**: 15-30 minutes for detailed specification.

### Approach 3: Legacy prompt (backward compatibility)
Use the prompt template below for features with existing specs.

## Prompt template (feature implementation)

Use this as a **single** copy/paste prompt to implement a feature **from a spec**.
Edit only the header block.

### Prompt (copy/paste)

```
FEATURE (fill this header)
- Name:
- Spec: spec/features/.../*.md
- OpenAPI operationId(s):
- HTTP endpoints:
- Outbound needs: (DB / filesystem / external HTTP)

TASK
Implement this feature end-to-end.

Source of truth to follow:
- This file: `AGENTS.md` (architecture + conventions)
- DoD: `docs/dod.md`
- Error contract: `docs/error-model.md`
- OpenAPI/REST conventions: `docs/openapi-guidelines.md`
- Testing rules: `docs/testing-strategy.md`
- Feature spec: the file in the header (`spec/features/**`)
- API contract: `api/openapi.yaml`

Deliverables
1) Spec alignment
   - Implement exactly what the feature spec says.
   - If the spec is ambiguous, update the spec in-place BEFORE coding.

2) Contract-first
   - Update `api/openapi.yaml` to match the final behavior (schemas, status codes, examples, tags, operationId).
   - Apply the conventions in `docs/openapi-guidelines.md`.

3) Code (hexagonal)
   - REST adapter in `it.quix.nomecliente.application` (thin, OpenAPI annotations).
   - IN port(s) in `it.quix.nomecliente.domain.port.in`.
   - Use case interface + `*Impl` in `it.quix.nomecliente.domain.usecase` (business logic in `*Impl`).
   - OUT port(s) in `it.quix.nomecliente.domain.port.out`.
   - Infrastructure implementations in `it.quix.nomecliente.infrastructure`.

4) Persistence / storage
   - If DB schema changes are needed: add Liquibase changeset under `src/main/resources/db/changelog/changes/**` and wire it in master.
   - If external HTTP integrations are needed: implement an adapter and stub it in tests with WireMock.

5) REST adapter
   - Implement the IN port in `application/*RestAdapter`.
   - Keep the REST adapter thin and consistent with the OpenAPI contract.
   - Add OpenAPI annotations as defined in `docs/openapi-guidelines.md`.

Quality gates (must be green)
- `mvn verify`

Output
- Provide a short summary of what you changed and where (paths + why).
```

### Example

```
FEATURE (fill this header)
- Name: Create folder
- Spec: spec/features/folders/10-api-create-folder.md
- OpenAPI operationId(s): createFolder
- HTTP endpoints: POST /api/folders
- Outbound needs: DB (folders table)

TASK
(Use the prompt above.)
```
