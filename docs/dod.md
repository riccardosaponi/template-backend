# Definition of Done (DoD)

A change is DONE when:

## Build & tests
- `./mvnw test` passes
- `./mvnw -Pintegration test` passes
- New features include unit + integration tests

## Contract & docs
- OpenAPI updated when endpoints change
- Relevant specs under `spec/` updated/added

## Database
- Liquibase changesets included for schema changes
- Schema can be built from scratch (no manual DB steps)

## Safety
- No real credentials committed
- No live external calls from tests
