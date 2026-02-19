# Quick start

## Project
- **GroupId**: `it.quix`
- **ArtifactId**: `nomecliente-backend-core`
- **Base package**: `it.quix.nomecliente`

## Configuration
This template uses a **single** `src/main/resources/application.yml`.
All settings are controlled via environment variables with defaults.

## Package layout (hexagonal)

```
it.quix.nomecliente/
├── application/               # REST adapters (controllers)
├── domain/
│   ├── port/in/               # IN ports (API contracts)
│   ├── usecase/               # use case interface + *Impl (business logic)
│   ├── port/out/              # OUT ports (DB/storage/external clients)
│   └── ddd/                   # domain model: dto/entity/enumeration
├── infrastructure/            # implementations of port.out (JPA/JdbcTemplate/clients/storage)
└── config/                    # Spring wiring
```

Key rules:
- REST adapters implement `domain.port.in.*` and **delegate**.
- Business logic lives in `domain.usecase.*Impl`.
- Outbound operations are expressed as interfaces in `domain.port.out.*`.
- Infrastructure provides implementations of `domain.port.out.*`.

## Run locally

```bash
docker compose up -d
./mvnw spring-boot:run
```

## Full local stack (Keycloak + WireMock + Mailpit + nginx HTTPS)

```bash
cd infra && docker compose up -d
```

Services started:

| Service | URL |
|---|---|
| PostgreSQL 17 | `localhost:5432` |
| Keycloak admin | `http://localhost:8180/admin` (admin/admin) |
| Keycloak HTTPS | `https://localhost:8443` |
| WireMock | `http://localhost:9090/__admin/ui` |
| Mailpit | `http://localhost:8025` |
| Hub (index) | `http://localhost:80` |

See [`infra/README.md`](infra/README.md) for full documentation.

Health:
- `GET http://localhost:8080/actuator/health`
- `GET http://localhost:8080/api/health`

Example API:
- `POST http://localhost:8080/api/v1/entities` - Create entity
- `GET http://localhost:8080/api/v1/entities` - List entities
- `GET http://localhost:8080/api/v1/entities/{id}` - Get entity by ID
- `PUT http://localhost:8080/api/v1/entities/{id}` - Update entity
- `DELETE http://localhost:8080/api/v1/entities/{id}` - Delete entity (logical)

## Tests

```bash
./mvnw test
```

## Where to start
- OpenAPI contract: `api/openapi.yaml`
- Feature specs: `spec/features/entities/**`
- DB migrations: `src/main/resources/db/changelog/changes/**`
- Example implementation: `Entity` (complete CRUD with tests)
