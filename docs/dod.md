# Definition of Done (DoD)

A feature is **DONE** only when **all** of the following criteria are verified.
Each criterion is formulated so that an agent can inspect the code or run a command
and answer: **PASS** or **FAIL**.

---

## 1. Build & Test

### 1.1 Clean build
- `./mvnw -Pintegration test` exits with **code 0**.
- No compilation errors. Warnings are not blocking but must be justified
  (`@SuppressWarnings` must not be used indiscriminately).

### 1.2 Integration tests (`*IT.java`)

Reference: [`docs/testing-strategy.md`](testing-strategy.md)

- For every new use case a `<UseCaseName>IT.java` file exists in
  `src/test/java/.../domain/usecase/`.
- Every test class extends `BaseUseCaseIT`.
- Every test method follows the naming convention:
  `methodName_should<Result>_when<Condition>()`.
- The following scenarios are covered (where applicable to the operation):

  | Scenario                        | Example                                                    |
  |---------------------------------|------------------------------------------------------------|
  | Happy path                      | create with valid data → entity present in DB              |
  | Entity not found                | non-existent ID → `ResourceNotFoundException`              |
  | Input validation                | blank / too-long field → 400 error                         |
  | Business rule violation         | duplicate code → `BusinessRuleViolationException`          |
  | Pagination (List only)          | page 0, page 1, last page with remaining elements          |
  | Sorting (List only)             | ASC and DESC on at least one field                         |
  | Full field coverage (List/Get)  | every DTO field verified against its expected value        |

- Every write-operation test verifies **actual persistence** by re-reading from
  the DB via the corresponding `get` use case (the write return value alone is
  not trusted).

### 1.3 Code coverage (JaCoCo)
- Classes in `domain.usecase` packages must have line coverage **≥ 30 %**.
- Report available at `target/site/jacoco/index.html` after the build.

---

## 2. Hexagonal Architecture

Reference: [`docs/architecture.md`](architecture.md)

### 2.1 IN Port (`domain.port.in`)
- One interface exists for **every operation** of the feature
  (e.g. `CreateEntityIn`, `ListEntitiesIn`).
- The interface has exactly **one method** whose signature is identical to the
  corresponding public method of the REST adapter.
- The method has **no** Spring MVC parameters (`@RequestParam`, `@PathVariable`,
  etc.): the signature is pure domain logic.
- For pagination, parameters are primitives: `int page, int size,
  String sortBy, String sortDirection` (not `Pageable`), unless an explicit
  exception is documented in `architecture.md`.

### 2.2 REST Adapter (`application.*RestAdapter`)
- The `RestAdapter` `implements` all IN ports of the feature.
- Every REST method has `@Override` implementing the IN port.
- The `RestAdapter` is **thin** — it contains only:
  - Spring MVC annotations (`@GetMapping`, etc.) and OpenAPI annotations.
  - Structural validation (`@Valid`).
  - `Pageable` / `Sort` construction when needed.
  - A single call to the use case.
  - HTTP response construction (`ResponseEntity`).
- **No** business logic in the `RestAdapter`.
- **No** `try/catch` in the `RestAdapter`: error handling is delegated to
  `GlobalExceptionHandler`.

### 2.3 Use Case (`domain.usecase`)
- A `<Name>UseCase` interface exists with an `execute(...)` method.
- A `<Name>UseCaseImpl` implementation annotated with `@Service` exists.
- All business logic (domain validations, transformations, rules) lives in the
  implementation.

### 2.4 OUT Port and Infrastructure
- If the feature accesses the DB, an OUT port interface exists in
  `domain.port.out` (e.g. `EntityRepositoryOut`) with the required methods.
- The JDBC implementation lives in `infrastructure.persistence`.
- The persistence model is confined to the infrastructure layer and never leaks
  into the domain (the domain model in `domain.ddd` is used instead).

---

## 3. OpenAPI Contract

Reference: [`docs/openapi-guidelines.md`](openapi-guidelines.md), [`api/openapi.yaml`](../api/openapi.yaml)

### 3.1 `api/openapi.yaml`
- Every new endpoint is present in the file.
- Every operation has a stable camelCase `operationId`
  (e.g. `createEntity`, `listEntities`).
- Every operation is assigned to **exactly one tag** consistent with the code
  annotation (`@Tag(name = "...")`).
- Every `2xx` response includes: `description`, `content`, `schema`, and at
  least one `example` with a realistic payload.
- Every `4xx`/`5xx` response uses the `ErrorResponse` schema defined in
  [`docs/error-model.md`](error-model.md) (fields: `code`, `message`, `details`, `correlationId`).

### 3.2 SpringDoc annotations in code
- The `RestAdapter` has `@Tag(name = "...", description = "...")` matching the
  tag entry in `api/openapi.yaml`.
- Every method has `@Operation(operationId = "...", summary = "...",
  description = "...")`.
- The `operationId` in code **exactly matches** the `operationId` in
  `api/openapi.yaml`.
- Every method has `@ApiResponses` with at least:
  - The success response (`2xx`).
  - `400` for operations with a request body.
  - `404` for operations on a single resource (`/{id}`).

---

## 4. Database

Reference: [`docs/liquibase-guidelines.md`](liquibase-guidelines.md)

### 4.1 Liquibase changeset
- For every schema change a **new** file exists in
  `src/main/resources/db/changelog/changes/`.
- Naming: `XXX-description-kebab-case.yaml` where `XXX` is the sequential
  number following the last existing file.
- The changeset has a **unique and descriptive `id`** (e.g. `create-entities-table`)
  and a filled `author`.
- The file is included in `db/changelog/db.changelog-master.yaml`.
- Existing changesets are **never modified**: corrections require a new changeset.
- Where relevant, the changeset includes a `rollback` section.

### 4.2 Idempotency check
- The schema builds from scratch without manual intervention: the command
  `./mvnw -Pintegration test` (Testcontainers with empty DB) passes.

---

## 5. Error Handling

Reference: [`docs/error-model.md`](error-model.md)

- Use cases throw only domain exceptions: `ResourceNotFoundException`,
  `BusinessRuleViolationException`, `ForbiddenException`, `IllegalArgumentException`.
- **No** `try/catch` in `RestAdapter`s: every unhandled exception propagates to
  `GlobalExceptionHandler`.
- Error responses always follow the `ErrorResponseDTO` format:
  `{ code, message, details?, correlationId }`.
- Error codes used are those defined in [`docs/error-model.md`](error-model.md)
  (e.g. `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`, `BUSINESS_RULE_VIOLATION`).

---

## 6. Security

Reference: [`docs/security-keycloak.md`](security-keycloak.md)

- All new endpoints under `/api/**` require JWT authentication
  (no `permitAll` added for business endpoints).
- No hardcoded credentials in source code or committed configuration files
  (passwords, secrets, tokens, API keys).
- Tests make no HTTP calls to real external services (Keycloak or others):
  `application-integration.yml` is used with JWT disabled / mocked.

---

## 7. Specifications (`spec/`)

- If the feature introduces or modifies a domain entity, the file
  `spec/features/<name>/<name>.md` is created or updated with:
  - Description of the entity and its fields.
  - Business rules.
  - API endpoints (method, path, request, response, errors).
- The spec content is consistent with the actual implementation
  (field names, types, constraints).
