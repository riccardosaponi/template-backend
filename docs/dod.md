# Definition of Done (DoD)

Una feature è **DONE** solo quando **tutti** i criteri seguenti sono verificati.
Ogni criterio è formulato in modo che un agente possa ispezionare il codice o
eseguire un comando e rispondere: **PASS** o **FAIL**.

---

## 1. Build & Test

### 1.1 Build pulito
- `./mvnw -Pintegration test` termina con **exit code 0**.
- Nessun errore di compilazione. I warning non bloccano ma devono essere
  giustificati (non `@SuppressWarnings` usati in modo indiscriminato).

### 1.2 Integration Test (`*IT.java`)

Riferimento: `docs/testing-strategy.md`

- Per ogni nuovo use case esiste un file `<NomeUseCase>IT.java` in
  `src/test/java/.../domain/usecase/`.
- Ogni classe di test estende `BaseUseCaseIT`.
- Ogni metodo di test segue la naming convention:
  `methodName_should<Result>_when<Condition>()`.
- Sono coperti i seguenti scenari (se applicabili all'operazione):

  | Scenario                  | Esempio                                          |
  |---------------------------|--------------------------------------------------|
  | Happy path                | creazione con dati validi → entity presente in DB |
  | Entità non trovata        | ID inesistente → `ResourceNotFoundException`     |
  | Validazione input         | campo blank/troppo lungo → errore 400            |
  | Business rule violation   | codice duplicato → `BusinessRuleViolationException` |
  | Paginazione (solo List)   | pagina 0, pagina 1, ultima pagina con elementi residui |
  | Sorting (solo List)       | ASC e DESC su almeno un campo                    |
  | Campi completi (solo List/Get) | tutti i campi del DTO verificati sul valore atteso |

- Ogni test di write operation verifica la **persistenza effettiva** rileggendo
  dal DB con il corrispondente `get` use case (non ci si fida solo del valore
  di ritorno della write).

### 1.3 Copertura del codice (JaCoCo)
- Le classi nei package `domain.usecase` devono avere line coverage **≥ 80 %**.
- Report verificabile aprendo `target/site/jacoco/index.html` dopo il build.

---

## 2. Architettura Esagonale

Riferimento: `docs/architecture.md`, `docs/hexagonal-pattern.md`

### 2.1 IN Port (`domain.port.in`)
- Esiste un'interfaccia per **ogni operazione** della feature
  (es. `CreateEntityIn`, `ListEntitiesIn`).
- L'interfaccia ha esattamente **un metodo** con firma identica a quella del
  metodo pubblico del REST adapter.
- Il metodo **non** ha parametri Spring MVC (`@RequestParam`, `@PathVariable`,
  ecc.): la firma è pura logica di dominio.
- Per la paginazione i parametri sono primitivi: `int page, int size,
  String sortBy, String sortDirection` (non `Pageable`), a meno di un
  accordo esplicito documentato in `architecture.md`.

### 2.2 REST Adapter (`application.*RestAdapter`)
- Il RestAdapter `implements` tutti gli IN port della feature.
- Ogni metodo REST ha `@Override` che implementa l'IN port.
- Il RestAdapter è **thin**: contiene solo:
  - Annotazioni Spring MVC (`@GetMapping`, ecc.) e OpenAPI.
  - Validazione strutturale (`@Valid`).
  - Costruzione di `Pageable` / `Sort` se necessario.
  - Una singola chiamata al use case.
  - Costruzione della risposta HTTP (`ResponseEntity`).
- **Nessuna** logica di business nel RestAdapter.
- **Nessun** `try/catch` nel RestAdapter: la gestione errori è delegata a
  `GlobalExceptionHandler`.

### 2.3 Use Case (`domain.usecase`)
- Esiste un'interfaccia `<Nome>UseCase` con il metodo `execute(...)`.
- Esiste un'implementazione `<Nome>UseCaseImpl` annotata con `@Service`.
- Tutta la business logic (validazioni di dominio, trasformazioni, regole) è
  nell'implementazione.

### 2.4 OUT Port e Infrastruttura
- Se la feature accede al DB, esiste un'interfaccia OUT port in
  `domain.port.out` (es. `EntityRepositoryOut`) con i metodi necessari.
- L'implementazione JPA / JDBC è in `infrastructure.persistence`.
- L'entità JPA è in `infrastructure.persistence.jpa.entity` e non trapela
  fuori dal layer infrastructure (si usa il domain model in `domain.ddd`).

---

## 3. Contratto OpenAPI

Riferimento: `docs/openapi-guidelines.md`, `api/openapi.yaml`

### 3.1 File `api/openapi.yaml`
- Ogni nuovo endpoint è presente nel file.
- Ogni operazione ha un `operationId` stabile in camelCase
  (es. `createEntity`, `listEntities`).
- Ogni operazione è assegnata a **esattamente un tag** coerente con quello
  nel codice (`@Tag(name = "...")`).
- Ogni risposta `2xx` contiene: `description`, `content`, `schema`,
  almeno un `example` con payload realistico.
- Ogni risposta `4xx`/`5xx` usa lo schema `ErrorResponse` definito in
  `docs/error-model.md` (campi: `code`, `message`, `details`, `correlationId`).

### 3.2 Annotazioni SpringDoc nel codice
- Il RestAdapter ha `@Tag(name = "...", description = "...")` identico
  all'entry del tag in `api/openapi.yaml`.
- Ogni metodo ha `@Operation(operationId = "...", summary = "...",
  description = "...")`.
- `operationId` nel codice **corrisponde esattamente** all'`operationId` in
  `api/openapi.yaml`.
- Ogni metodo ha `@ApiResponses` con almeno:
  - La risposta di successo (`2xx`).
  - `400` per operazioni con request body.
  - `404` per operazioni su risorsa singola (`/{id}`).

---

## 4. Database

Riferimento: `docs/liquibase-guidelines.md`

### 4.1 Changeset Liquibase
- Per ogni modifica allo schema esiste un **nuovo** file in
  `src/main/resources/db/changelog/changes/`.
- Naming: `XXX-descrizione-kebab-case.yaml` dove `XXX` è il numero
  sequenziale progressivo rispetto all'ultimo file esistente.
- Il changeset ha `id` **univoco e descrittivo** (es. `create-entities-table`)
  e `author` compilato.
- Il file è incluso in `db/changelog/db.changelog-master.yaml`.
- I changeset esistenti **non vengono mai modificati**: eventuali correzioni
  si fanno con un nuovo changeset.
- Se pertinente, il changeset include la sezione `rollback`.

### 4.2 Verifica idempotenza
- Lo schema si costruisce da zero senza interventi manuali: il comando
  `./mvnw -Pintegration test` (che usa Testcontainers con DB vuoto) passa.

---

## 5. Gestione Errori

Riferimento: `docs/error-model.md`

- Le eccezioni lanciate dai use case sono esclusivamente le exception di
  dominio: `ResourceNotFoundException`, `BusinessRuleViolationException`,
  `ForbiddenException`, `IllegalArgumentException`.
- **Nessun** `try/catch` nei RestAdapter: ogni eccezione non gestita sale fino
  a `GlobalExceptionHandler`.
- Le risposte di errore hanno sempre il formato `ErrorResponseDTO`:
  `{ code, message, details?, correlationId }`.
- I codici di errore usati sono quelli definiti in `docs/error-model.md`
  (es. `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`, `BUSINESS_RULE_VIOLATION`).

---

## 6. Sicurezza

Riferimento: `docs/security-keycloak.md`

- Tutti i nuovi endpoint in `/api/**` richiedono autenticazione JWT
  (nessun `permitAll` aggiunto per endpoint business).
- Nessuna credenziale hardcodata nel codice sorgente o nei file di
  configurazione committati (password, secret, token, chiavi API).
- I test non effettuano chiamate HTTP verso servizi esterni reali
  (Keycloak o altri): si usa `application-integration.yml` con JWT
  disabilitato / mocked.

---

## 7. Specifiche (`spec/`)

- Se la feature introduce o modifica un'entità di dominio, il file
  `spec/features/<nome>/<nome>.md` è aggiornato o creato con:
  - Descrizione dell'entità e dei suoi campi.
  - Business rules.
  - Endpoint API (metodo, path, request, response, errori).
- Il contenuto dello spec è coerente con l'implementazione effettiva
  (nomi dei campi, tipi, vincoli).
