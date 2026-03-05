# AI Agent Instructions: ${PROJECT_NAME} Engineering Standards

## Project Identity
- **Project Name**: `${PROJECT_NAME}`
- **Base Package**: `${BASE_PACKAGE}` (e.g. `it.quix.nomecliente`)
- **Group ID**: `${GROUP_ID}`
- **Artifact ID**: `${ARTIFACT_ID}`
- **Domain Library**: `${DOMAIN_LIB}` (e.g. `folders`)

---

## Purpose
This document guides an AI assistant to:
1. Implement features end-to-end following the hexagonal architecture and conventions of this codebase.
2. Create new project skeletons that respect the same engineering standards.

The project is a **multi-module Spring Boot monorepo** (single application, multiple Maven modules).

---

## Quick Commands
- Compile: `./mvnw clean compile`
- Tests + coverage: `./mvnw -q verify`
- Run locally (with docker-compose Postgres): `./mvnw spring-boot:run -pl app`

---

## Non-Negotiable Principles
- **Consistency**: Keep naming, packaging, layering, logging, validation, and error handling consistent.
- **Quality first**: Green build, small but meaningful tests, avoid unnecessary warnings.
- **Minimal, focused changes**: Don't introduce unrelated refactors or formatting changes.
- **Reproducibility**: Declare all dependencies via Maven; avoid manual, ad-hoc steps.
- **Security**: Never log secrets; prevent SQL injection (use named parameters); validate all inputs.
- **Edit only what's necessary**: Modify only files directly relevant to the task. Do not edit `pom.xml` or documentation unless explicitly required by the task.

---

## Code Formatting & Style

### Formatting Standards
- **Indentation**: 4 spaces (IntelliJ IDEA default).
- **Line Length**: Maximum 120 characters.
- **Encoding**: UTF-8.

### Java Style Guidelines
- Use descriptive names for classes, methods, and variables.
- **Avoid `var`**: Prefer explicit types for clarity.
- **Immutability first**: Avoid mutations, especially in Stream API or for-each loops.
- **No magic numbers/strings**: Use named constants.
- **Early returns**: Prefer early returns; avoid `else` when not necessary.
- **No `throws` clause**: Prefer unchecked exceptions.
- All code, identifiers, comments, and docs must be in **English**.

---

## Architecture (Hexagonal / DDD — Multi-Module)

### Repository Layout
```
<repo-root>/
  pom.xml                            # Parent POM (packaging=pom), modules: libs/shared-core, libs/${DOMAIN_LIB}-domain, app

  app/                               # Spring Boot runnable module (artifactId: ${project}-app)
    src/main/java/${BASE_PACKAGE_PATH}/
      backoffice/                    # REST Adapters (thin controllers, implement IN ports)
      config/                        # Spring configuration, explicit bean wiring
      config/security/               # SecurityConfig (SecurityFilterChain)
    src/main/resources/
      application.yml
      db/changelog/                  # Liquibase master + changesets

  libs/
    shared-core/                     # Cross-cutting concerns (artifactId: ${project}-shared-core)
      src/main/java/${BASE_PACKAGE_PATH}/
        config/                      # WebConfig (CORS), GlobalExceptionHandler
        config/security/             # JWT classes, SecurityContextHelper
        domain/exception/            # Shared runtime exceptions
        domain/ddd/dto/error/        # Error response DTOs

    ${DOMAIN_LIB}-domain/            # Domain module per feature area (artifactId: ${project}-${DOMAIN_LIB}-domain)
      src/main/java/${BASE_PACKAGE_PATH}/
        domain/port/in/              # IN ports (use case interfaces)
        domain/port/out/             # OUT ports (repository/client interfaces)
        domain/usecase/              # Use case interface + *Impl (business logic)
        domain/ddd/dto/              # Request/Response DTOs
        domain/ddd/entity/           # Domain entities
        domain/ddd/enumeration/      # Enumerations
        infrastructure/              # OUT port implementations (JDBC, HTTP clients, storage)
```

> Maven module naming convention: `${project}-shared-core`, `${project}-${DOMAIN_LIB}-domain`, `${project}-app`.

### Layer Rules

| Layer | Module / Package | Rule |
|---|---|---|
| REST Adapter | `app/.../backoffice/` | Thin: HTTP mapping + validation + delegation only. Implements IN port. |
| IN Port | `libs/${DOMAIN_LIB}-domain/.../domain/port/in/` | Pure interface defining the use case contract. |
| Use Case | `libs/${DOMAIN_LIB}-domain/.../domain/usecase/` | Interface + `*Impl`. Business logic lives in `*Impl`. |
| OUT Port | `libs/${DOMAIN_LIB}-domain/.../domain/port/out/` | Interface for persistence, filesystem, external HTTP. |
| Infrastructure | `libs/${DOMAIN_LIB}-domain/.../infrastructure/` | Implements OUT ports (JDBC, HTTP client, storage). |
| Shared Utils | `libs/shared-core/.../config/` | WebConfig, GlobalExceptionHandler, JWT helpers. No business logic. |
| Config | `app/.../config/` | Spring `@Configuration` classes and `SecurityFilterChain`. No business logic. |

> Rule: REST adapters must not contain business rules. Those belong in `*Impl`.

---

## Database Conventions (JDBC + Liquibase)

### Technology
- **Use `NamedParameterJdbcTemplate` with SQL text blocks (`"""`)**. Never use JPA/Hibernate.

### Liquibase
- Master changelog: `app/src/main/resources/db/changelog/db.changelog-master.yaml`
- Changesets: `app/src/main/resources/db/changelog/changes/**`
- Format: XML changesets (`.xml`).

### Column Types (always lowercase)
| Value | Type |
|---|---|
| GUID / UUID | `varchar(36)` — never `uuid` |
| Timestamps | `timestamptz` — never `timestamp` |
| Strings | `varchar(n)` |
| Flags / states | `int` or `boolean` |
| Large numbers | `bigint` |

### Table and Column Naming
- **Table names**: singular form (`folder`, `file`, not `folders`).
- **Primary key**: `${entity}_id` (e.g. `folder_id`).
- **Column names**: `snake_case`.

### Standard Audit Columns (mandatory on every table)
```xml
<column name="created_by"   type="varchar(255)"><constraints nullable="false"/></column>
<column name="created_date" type="timestamptz"><constraints nullable="false"/></column>
<column name="updated_by"   type="varchar(255)"/>
<column name="updated_date" type="timestamptz"/>
<column name="canceled"     type="int" defaultValueNumeric="0"><constraints nullable="false"/></column>
<column name="delete_date"  type="timestamptz"/>
<column name="delete_user"  type="varchar(100)"/>
```

### Soft Delete Pattern
- **Never physically delete records**.
- Set `canceled = 1`, populate `delete_date` and `delete_user`.
- All SELECT queries must filter with `WHERE canceled = 0`.

---

## Lombok Best Practices
- **DTOs**: `@Getter` + `@Setter`. Avoid `@Data`.
- **Services / Use cases**: `@RequiredArgsConstructor` for constructor injection.
- **Logging**: `@Slf4j`.

---

## REST API Conventions

### Location
REST adapters (controllers) belong in the `app/` module under `backoffice/`. They implement the IN port from the corresponding `libs/` module.

### HTTP Semantics
| Method | Success Status |
|---|---|
| GET | 200 OK or 204 No Content |
| POST | 201 Created |
| PUT / PATCH | 200 OK |
| DELETE | 204 No Content (soft delete) |

### Other Rules
- **No `@CrossOrigin`**: CORS is managed centrally in `WebConfig.java` (in `shared-core`).
- **File download**: Return `ResponseEntity<Resource>` with `Content-Disposition`. Stream directly; never load entire file into memory.
- Apply OpenAPI annotations as defined in `docs/openapi-guidelines.md`.

---

## Testing Strategy

### Unit Tests (domain / use case layer)
- Location: same module as the code under test.
- Annotation: `@ExtendWith(MockitoExtension.class)`.
- Mock all outbound ports (OUT ports).
- Cover: happy path + all error/edge cases.
- Naming: `shouldXxx()` or `xxx_shouldYyy()`.

### Integration Tests (full slice)
- Use Spring Boot context + PostgreSQL via **Testcontainers**.
- Liquibase must apply schema from scratch on each test run.
- Use **MockMvc** to test endpoints end-to-end.
- External HTTP APIs must be stubbed with **WireMock** (never call live systems in CI).
- Use `@TempDir` for isolated filesystem testing.

### Required Scenarios (baseline per endpoint)
- Success path
- Validation error → 400
- Authorization → 403 (once security is enabled)
- Domain edge case (conflicts, not-found, business rule violations, etc.)

### Acceptance Criteria
1. Code compiles (`./mvnw clean compile`).
2. All tests pass — no skipped tests.
3. Happy path AND error scenarios covered.
4. No new warnings introduced.

---

## Spec-Driven Workflow

### Artifacts
| Artifact | Path | Role |
|---|---|---|
| Feature spec | `spec/features/**/*.md` | Business + API + persistence requirements |
| API contract | `api/openapi.yaml` | Source of truth for endpoints and payloads |
| DoD | `docs/dod.md` | Definition of Done |
| Error contract | `docs/error-model.md` | Error response format |
| OpenAPI conventions | `docs/openapi-guidelines.md` | Annotation and schema rules |
| Testing rules | `docs/testing-strategy.md` | Test patterns and requirements |
| Quick prompts | `docs/prompts/quick-prompts.md` | Ready-to-use templates for common patterns |
| Blueprint | `docs/prompts/rest-api-blueprint.md` | Comprehensive template for complex features |

### Implementation Approaches

#### Approach 1 — Quick patterns (recommended for common cases)
Use `docs/prompts/quick-prompts.md`. Choose from 7 standard patterns:
1. CRUD Entity with database
2. Search/Filter API
3. File Upload with metadata
4. Aggregation/Statistics
5. Batch Processing
6. Entity Relationship Management
7. State Machine/Workflow

Time: 2–5 minutes to customize and use.

#### Approach 2 — Custom feature (complex scenarios)
Use `docs/prompts/rest-api-blueprint.md`.
Covers: business requirements, API contract, data model, acceptance criteria, testing requirements, implementation checklist.
Time: 15–30 minutes.

---

## Feature Implementation Prompt Template

Use this as a **single copy/paste prompt** to implement a feature from a spec. Edit only the header block.

```
FEATURE
- Name:
- Spec: spec/features/.../*.md
- OpenAPI operationId(s):
- HTTP endpoint(s):
- Outbound needs: (DB / filesystem / external HTTP)

TASK
Implement this feature end-to-end following the conventions in AGENTS.md.

Source of truth:
- Architecture + conventions: AGENTS.md
- DoD: docs/dod.md
- Error contract: docs/error-model.md
- OpenAPI conventions: docs/openapi-guidelines.md
- Testing rules: docs/testing-strategy.md
- Feature spec: (file in the header)
- API contract: api/openapi.yaml

Deliverables

1) Spec alignment
   - Implement exactly what the spec says.
   - If the spec is ambiguous, update it in-place BEFORE coding.

2) Contract-first
   - Update api/openapi.yaml to match the final behavior (schemas, status codes, examples, tags, operationId).
   - Apply conventions from docs/openapi-guidelines.md.

3) Code (hexagonal, multi-module)
   - REST adapter in app/.../backoffice/ (thin, OpenAPI annotations, implements IN port).
   - IN port in libs/${DOMAIN_LIB}-domain/.../domain/port/in/.
   - Use case interface + *Impl in libs/${DOMAIN_LIB}-domain/.../domain/usecase/ (business logic in *Impl).
   - OUT port in libs/${DOMAIN_LIB}-domain/.../domain/port/out/.
   - Infrastructure implementations in libs/${DOMAIN_LIB}-domain/.../infrastructure/.
   - Shared exceptions/error DTOs in libs/shared-core/.../domain/exception/ and domain/ddd/dto/error/.

4) Persistence / storage
   - DB schema changes: add Liquibase XML changeset under `app/src/main/resources/db/changelog/changes/`
     and wire it in the master changelog.
   - External HTTP: implement adapter + stub with WireMock in tests.

5) Tests
   - Unit tests (Mockito) for use case logic.
   - Integration tests (Testcontainers + MockMvc) for the full slice.
   - Cover: success path, 400 validation, 403 authorization, domain edge case.

Quality gate (must be green)
- ./mvnw verify

Output
- Short summary: what changed, where, and why. - Build/test status (PASS / FAIL). If FAIL, provide fix plan.
```

### Example

```
FEATURE
- Name: Create folder
- Spec: spec/features/folders/10-api-create-folder.md
- OpenAPI operationId(s): createFolder
- HTTP endpoint(s): POST /api/folders
- Outbound needs: DB (folder table)

TASK
(use the template above)
```

---

## What NOT To Do
- **No JPA/Hibernate**: use `NamedParameterJdbcTemplate` with SQL text blocks.
- **No `@CrossOrigin`**: CORS is centralized.
- **No `var`**: use explicit types.
- **No physical deletes**: always soft delete.
- **No live external calls in tests**: use WireMock or Mockito stubs.
- **No `@Data` on DTOs**: use `@Getter` + `@Setter`.
- **No new tooling/config** (linters, plugins) unless explicitly requested.
- **No package or naming convention changes** by preference.
- **No edits to unrelated files** (avoid `pom.xml` bloat).
- **No secrets in logs**.

---

## Expected AI Output Format

Each changeset must end with:
1. **Summary**: files modified/created and why.
2. **Build/test status**: PASS or FAIL. If FAIL, provide the fix plan.
3. **Next steps** (optional): what should be done next.
