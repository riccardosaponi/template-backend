# Backend Spring Boot Template (PostgreSQL + Liquibase)

**Architecture**: Hexagonal Ports & Adapters

This repository is a **template** for backend services.
It provides:
- Spring Boot service skeleton with **Entity CRUD** as example
- **Spring Security + Keycloak JWT** authentication
- PostgreSQL local dev via Docker Compose
- Liquibase migrations
- OpenAPI contract
- Integration testing setup (Testcontainers for PostgreSQL)
- Spec-driven documentation structure
- Health check endpoint

## Project Info
- **GroupId**: `it.quix`
- **ArtifactId**: `nomecliente-backend-core`
- **Base Package**: `it.quix.nomecliente`

## Example Implementation
This template includes a complete CRUD implementation for a **Entity** with:
- `id` (UUID)
- `code` (String, unique)
- `description` (String)
- Audit fields: `createDate`, `createUser`, `lastUpdateDate`, `lastUpdateUser`
- Logical delete: `canceled` (Boolean)

This serves as a reference implementation following hexagonal architecture patterns.

## Configuration
This template uses a **single** `src/main/resources/application.yml`.
All runtime settings are controlled via environment variables with sensible defaults (see `application.yml`).

## Architecture

### `application/` (driving adapter)
Contains REST adapters (controllers). They:
- are Spring `@RestController`
- implement **IN ports** from `domain.port.in`
- delegate to **use case implementations** (business logic)

### `domain/` (hexagon)
Contains the core contracts and business logic.

- `domain.port.in`: **IN ports** (API contracts). Pure interfaces used to define the REST API surface.
- `domain.usecase`: **use case interfaces** (business contracts) + their implementations.
  - Interface is used by the REST adapter
  - Implementation lives in the domain and contains business logic (suffix `Impl`)
- `domain.port.out`: **OUT ports** (interfaces for persistence, filesystem/storage, and external services)
- `domain.ddd`: domain model (`dto`, `entity`, `enumeration`)

### `infrastructure/` (driven adapters)
Contains implementations of `domain.port.out`, such as:
- JPA repositories
- `JdbcTemplate` repositories with plain SQL
- REST clients to third-party services
- filesystem/storage services

### `config/`
Spring beans wiring / configuration.

## Documentation
- **[AGENTS.md](AGENTS.md)** - Development workflow and repo conventions
- **[docs/architecture.md](docs/architecture.md)** - Hexagonal architecture structure
- **[docs/error-model.md](docs/error-model.md)** - Error handling standards
- **[docs/testing-strategy.md](docs/testing-strategy.md)** - Testing guidelines
- **[docs/openapi-guidelines.md](docs/openapi-guidelines.md)** - REST API conventions
- **[docs/security-keycloak.md](docs/security-keycloak.md)** - Authentication & authorization
- **[docs/liquibase-guidelines.md](docs/liquibase-guidelines.md)** - Database migration management
- **[docs/dod.md](docs/dod.md)** - Definition of Done checklist
- **[docs/prompts/](docs/prompts/)** - AI prompt blueprints for feature implementation

📖 **See [docs/README.md](docs/README.md) for complete documentation index**

## Quick Start for Developers

### New to the project?
1. Read **[docs/architecture.md](docs/architecture.md)** to understand the structure
2. Check **[AGENTS.md](AGENTS.md)** for development workflow

### Implementing a feature?
- **Common patterns** (CRUD, search, upload): Use [docs/prompts/quick-prompts.md](docs/prompts/quick-prompts.md) (2-5 min)
- **Complex features**: Use [docs/prompts/rest-api-blueprint.md](docs/prompts/rest-api-blueprint.md) (15-30 min)
- **Existing specs**: Follow [AGENTS.md](AGENTS.md) prompt template

### Need guidance?
- **Error handling**: [docs/error-model.md](docs/error-model.md)
- **REST APIs**: [docs/openapi-guidelines.md](docs/openapi-guidelines.md)
- **Database changes**: [docs/liquibase-guidelines.md](docs/liquibase-guidelines.md)
- **Testing**: [docs/testing-strategy.md](docs/testing-strategy.md)
- **Security**: [docs/security-keycloak.md](docs/security-keycloak.md)

## Quickstart
1. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
2. Run the service:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Healthcheck:
   - `GET http://localhost:8080/actuator/health`
   - `GET http://localhost:8080/api/health`

4. Try the example API:
   - `POST http://localhost:8080/api/v1/entities` - Create entity
   - `GET http://localhost:8080/api/v1/entities` - List entities
   - `GET http://localhost:8080/api/v1/entities/{id}` - Get by ID

## Run tests
- Integration tests:
  ```bash
  ./mvnw test
  ```
  ./mvnw -q test
  ```
- Integration tests (Testcontainers PostgreSQL):
  ```bash
  ./mvnw -q -Pintegration test
  ```

## Specs
- API contract: `api/openapi.yaml`
- Feature specs (API + business logic + persistence): `spec/features/**`
