# 🤖 AI Agent Instructions: ${PROJECT_NAME} Engineering Standards

## 📌 Project Identity
- **Project Name**: ${PROJECT_NAME}
- **Base Package**: `${BASE_PACKAGE}`
- **Group ID**: `${GROUP_ID}`
- **Artifact ID**: `${ARTIFACT_ID}`

---

## 🎯 Purpose
This document guides an AI assistant to:
1) Create a new project skeleton following the same engineering standards used in this codebase (architecture, conventions, quality gates);
2) Implement subsequent features while preserving those standards.

The new project contains **a single application** (single-service). There’s no need to manage multiple apps in the same repository.

---

## ⚖️ Non-Negotiable Principles
- **Consistency**: Keep naming, packaging, layering (core/domain/infrastructure), logging, validation, and error handling consistent.
- **Quality first**: Green build, small but meaningful tests, avoid unnecessary warnings.
- **Minimal, focused changes**: Don't introduce unrelated refactors or formatting changes.
- **Reproducibility**: Declare dependencies via Maven; avoid manual, ad-hoc steps.
- **Security**: Never log secrets; prevent SQL injection (use named parameters); validate inputs.
- **Edit only what's necessary**: Modify only files directly relevant to the task. Do not edit configuration files (like `pom.xml`) or documentation unless explicitly required.

---

## 💻 Code Formatting & Style

### Formatting Standards
- **Indentation**: 4 spaces (IntelliJ IDEA default).
- **Line Length**: Maximum 120 characters.
- **Encoding**: UTF-8.

### Java Style Guidelines
- Use descriptive names for classes, methods, and variables.
- **Avoid `var` keyword**: Prefer explicit types for clarity.
- **Preference for immutability**: Avoid mutations, especially in Stream API or for-each loops.
- **Avoid magic numbers/strings**: Use constants.
- **Early returns**: Prefer early returns; avoid `else` statements when not necessary.
- **Avoid `throws` clause**: Prefer unchecked exceptions.

---

## 🏗️ Architecture (Hexagonal / DDD)



### Repository Layout
```text
<repo-root>/
  pom.xml
  app/                     # Main Spring Boot Service
    src/main/java/${BASE_PACKAGE_PATH}/backoffice/   # REST Adapters (Controllers)
  libs/
    shared-core/           # Security, WebConfig, Global Exceptions
    ${DOMAIN_LIB}/         # Feature-specific logic
      src/main/java/${BASE_PACKAGE_PATH}/${DOMAIN_LIB}/
        port/in/           # Use Case Interfaces (Inbound Ports)
        port/out/          # Repository/Client Interfaces (Outbound Ports)
        usecase/           # Business logic implementations
        dto/               # Request/Response objects
        infrastructure/    # JDBC/HTTP Adapters

```

---

## 🗄️ Database Conventions (JDBC & Liquibase)

### Column Types (Always lowercase)

* **GUID/UUID**: Use `varchar(36)`, never `uuid` type.
* **Timestamps**: Use `timestamptz`, never `timestamp`.
* **Others**: `varchar(n)`, `boolean`, `bigint`, `int`.

### Table and Column Naming

* **Table names**: Use **singular** form (e.g., `folder`, `file`).
* **ID Column naming**: Primary key must be named `${entity}_id` (e.g., `folder_id`).
* **Column names**: Use **snake_case**.

### Standard Audit Columns

Every table must include:

```xml
<column name="created_by" type="varchar(255)"><constraints nullable="false"/></column>
<column name="created_date" type="timestamptz"><constraints nullable="false"/></column>
<column name="updated_by" type="varchar(255)"/><column name="updated_date" type="timestamptz"/>
<column name="canceled" type="int" defaultValueNumeric="0"><constraints nullable="false"/></column>
<column name="delete_date" type="timestamptz"/><column name="delete_user" type="varchar(100)"/>

```

### Soft Delete Pattern

* **NEVER physically delete records**.
* Use `canceled` int column (default `0`).
* When "deleting", set `canceled = 1` and populate `delete_date` and `delete_user`.
* Filter queries with `WHERE canceled = 0`.

---

## 🧪 Testing Strategy

* **Location**: Tests live in the same library module as the code.
* **Mock Strategy**: Use `@ExtendWith(MockitoExtension.class)`. Mock all outbound ports.
* **WireMock**: For external HTTP APIs (e.g., Keycloak), tests MUST stub HTTP with WireMock.
* **Filesystem**: Use `@TempDir` for isolated filesystem testing.
* **Naming**: Use `shouldXxx()` or `xxx_shouldYyy()`.

### Acceptance Criteria

1. ✅ Code compiles (`mvn clean compile`).
2. ✅ All tests pass (NO SKIPPED TESTS).
3. ✅ Happy path AND error scenarios covered.
4. ✅ No new warnings introduced.

---

## 🌐 REST API Conventions

* **Location**: REST Adapters (Controllers) belong in the `app/` module.
* **Interface**: Implement the `PortIn` interface from the `libs/` module.
* **CORS**: **Do NOT use `@CrossOrigin**`. Managed centrally in `WebConfig.java`.
* **Methods**:
* **GET**: 200 OK or 204 No Content.
* **POST**: 201 Created.
* **DELETE**: 204 No Content (Implementation: Soft Delete).


* **File Download**: Return `ResponseEntity<Resource>` with `Content-Disposition`. Never load the entire file into memory; stream directly.

---

## ⚙️ Lombok Best Practices

* **DTOs**: Use `@Getter` and `@Setter`. **Avoid `@Data**`.
* **Services**: Use `@RequiredArgsConstructor` for constructor injection.
* **Logging**: Use `@Slf4j`.

---

## 🚫 What NOT To Do

* Don't add new tools/config (linters, etc.) unless requested.
* Don't change conventions (packages, naming) "by preference".
* Don't use JPA/Hibernate. Use `NamedParameterJdbcTemplate` with text blocks (`"""`).
* Don't edit files not directly required by the task (avoid `pom.xml` bloat).

---

## 🏁 Expected AI Output

Each change set should end with:

* A short summary of modifications;
* Build/test status (PASS/FAIL) and, if FAIL, the fix plan;
* Optional next steps.

```