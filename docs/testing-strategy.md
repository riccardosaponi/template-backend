# Testing Strategy

## Overview
The project testing strategy is based on:
- **Integration tests**: end-to-end testing with a real database and full Spring context.
- **Code coverage**: automatic coverage monitoring via JaCoCo.

## Test Structure

### File Naming
- **Integration tests**: `*IT.java` (e.g. `CreateFolderUseCaseIT.java`)
  - Test complete use cases with a real database.
  - Load the full Spring Boot context.
  - Use Testcontainers for PostgreSQL.

### Folder Organisation
```
src/test/java/
└── it/quix/nomecliente/
    └── domain/
        └── usecase/          # Integration tests per use case
            ├── BaseUseCaseIT.java  # Shared base class
            └── *IT.java            # Test class per use case
```

## Unit Tests

Unit tests target the **domain and use case layer** in isolation. They run without a Spring context and without a database.

### Characteristics
- **No Spring context**: plain JUnit 5 + Mockito — no `@SpringBootTest`.
- **All OUT ports mocked**: the use case under test never touches a real database or external service.
- **Fast**: no I/O — run in milliseconds.
- **Location**: same Maven module as the code under test (`libs/{domain}-domain`).

### Setup

```java
@ExtendWith(MockitoExtension.class)
class CreateEntityUseCaseImplTest {

    @Mock
    private EntityRepositoryOut entityRepository;

    @InjectMocks
    private CreateEntityUseCaseImpl useCase;
}
```

### Naming Convention
`methodName_should<Result>_when<Condition>()`

Examples:
- `execute_shouldReturnCreatedEntity_whenRequestIsValid()`
- `execute_shouldThrowResourceNotFoundException_whenEntityNotFound()`

### Required Scenarios (baseline per use case)
| Scenario | Expected result |
|---|---|
| Happy path | Correct return value; OUT port called with expected arguments |
| Validation failure | Domain exception thrown before any OUT port call |
| Entity not found | `ResourceNotFoundException` thrown |
| Business rule violation | `BusinessRuleViolationException` thrown |
| Edge case | Boundary values, null handling, empty collections |

### What to verify
- Return value matches the expected domain object.
- OUT port methods called exactly the expected number of times (`verify(..., times(n))`).
- Correct exception type and message thrown.
- OUT port is **not** called when input validation fails (fail fast).

> **Note**: detailed code examples for unit tests are to be added. See the integration test examples below as reference for structure and assertion style.

---

## Integration Tests

### Characteristics
- **Full context**: load Spring Boot with `@SpringBootTest`.
- **Real database**: PostgreSQL via Testcontainers.
- **Automatic migrations**: Liquibase applies the schema from scratch.
- **Transaction management**: each test runs in a transaction (automatic rollback).

### Base Class: `BaseUseCaseIT`
All integration tests extend this class, which provides:
- Shared PostgreSQL container (singleton pattern for performance).
- Automatic datasource property configuration.
- Common setup for all tests.

```java
@SpringBootTest(classes = BackendApplication.class)
public abstract class BaseUseCaseIT {
    // PostgreSQL container singleton shared across all tests
    private static final PostgreSQLContainer<?> postgres;
    
    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Integration Test Example
```java
class CreateFolderUseCaseIT extends BaseUseCaseIT {

    @Autowired
    private CreateFolderUseCase createFolderUseCase;

    @Autowired
    private GetFolderUseCase getFolderUseCase;

    @Test
    void createFolder_should_persistCorrectly_when_validRequest() {
        // Given
        CreateFolderRequest request = new CreateFolderRequest("Test Folder", null);

        // When
        FolderDto result = createFolderUseCase.execute(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Folder");
    }
}
```

### Why Integration Tests?

Integration tests are **fundamental** because they verify the **end-to-end** behaviour of the application under realistic conditions, simulating the production environment. Unlike unit tests that isolate individual units of code with mocks, integration tests validate the entire call chain: from the REST API, through the business logic, down to persistence on a real database.

#### 1. Validate Real Component Integration
- **Database**: verify that SQL queries work correctly with a real database (PostgreSQL).
- **Transactions**: test that transactions are managed correctly (commit/rollback, isolation).
- **Spring Context**: ensure all dependencies are injected correctly and wiring works.
- **Migrations**: validate that Liquibase scripts are correct, applicable, and consistent with the code.
- **Configuration**: verify that Spring properties are correctly configured and applied.

#### 2. Discover Hidden Problems (that mocks do not catch)
- **ORM mapping**: mapping errors between JPA entities and database tables.
- **Database constraints**: unique constraint, foreign key, not null, check constraint violations.
- **Type mismatch**: type incompatibilities between Java and SQL (e.g. UUID vs String, Timestamp vs LocalDateTime).
- **Lazy loading**: `LazyInitializationException` when accessing relations outside a transactional context.
- **N+1 queries**: performance problems caused by multiple unoptimised queries.
- **Deadlocks**: database deadlock situations that are not detectable with mocks.

#### 3. Test Complete Business Logic
- **Validations**: both at the application level (`@Valid`) and at the database level (constraints).
- **Business rules**: rules involving multiple entities and coordinated operations.
- **Side effects**: effects on other related entities (cascade, triggers, audit).
- **Complete workflows**: flows that span multiple use cases (e.g. create parent → create child → verify relation).

#### 4. Guarantee Consistency Between Spec and Implementation
- **Database schema**: tables exist and have the correct columns.
- **API contract**: the OpenAPI contract is respected by the implementation.
- **Data consistency**: saved data respects business rules and defined constraints.

### What to Test in Integration Tests

Integration tests must focus on **observable final results** and **actual data persistence**. It is not enough to verify that a method does not throw an exception: you must validate that data was actually saved, modified, or deleted correctly in the database, and that all relations and constraints are respected.

#### Persistence and Data Retrieval

```java
@Test
void createFolder_should_persistCorrectly_when_validRequest() {
    // Given
    CreateFolderRequest request = new CreateFolderRequest("Test Folder", null);
    
    // When — persist the data
    FolderDto created = createFolderUseCase.execute(request);
    
    // Then — verify the data was saved correctly
    assertThat(created.getId()).isNotNull();
    assertThat(created.getName()).isEqualTo("Test Folder");
    
    // Verify retrieval from database
    FolderDto retrieved = getFolderUseCase.execute(created.getId());
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getName()).isEqualTo(created.getName());
}
```

**What to verify:**
- Auto-generated ID (if auto-increment).
- All fields populated correctly.
- Automatic timestamps (createdAt, updatedAt).
- Default values applied.
- Data actually retrievable from the database.

#### Relations Between Entities (Parent-Child)
```java
@Test
void createFolder_should_linkToParent_when_parentExists() {
    // Given
    FolderDto parent = fixtures.createFolder("Parent");
    
    // When — create child with reference to parent
    CreateFolderRequest request = new CreateFolderRequest("Child", parent.getId());
    FolderDto child = createFolderUseCase.execute(request);
    
    // Then — verify the relation
    assertThat(child.getParentId()).isEqualTo(parent.getId());
    
    // Verify the parent is accessible from the query
    FolderDto retrieved = getFolderUseCase.execute(child.getId());
    assertThat(retrieved.getParentId()).isEqualTo(parent.getId());
}
```

**What to verify:**
- Foreign key correctly saved.
- Bidirectional relation (if present).
- Cascade operations (if configured).
- Navigation between related entities.

#### Validations and Constraints
```java
@Test
void createFolder_should_throwException_when_parentNotFound() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    CreateFolderRequest request = new CreateFolderRequest("Test", nonExistentId);
    
    // When/Then — verify the correct exception is thrown
    assertThatThrownBy(() -> createFolderUseCase.execute(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Parent folder not found");
}

@Test
void createFolder_should_normalizeData_when_nameHasSpaces() {
    // Given
    CreateFolderRequest request = new CreateFolderRequest("  Test Folder  ", null);
    
    // When
    FolderDto result = createFolderUseCase.execute(request);
    
    // Then — verify normalisation (trim)
    assertThat(result.getName()).isEqualTo("Test Folder");
}
```

**What to verify:**
- Correct business exceptions (`ResourceNotFoundException`, `ValidationException`).
- Unique constraint violations.
- Not null violations.
- Data normalisation / sanitisation.
- Custom validations (`@Valid`, `@Validated`).

#### Transactions and Rollback
```java
@Test
void updateFolder_should_rollback_when_errorOccurs() {
    // Given
    FolderDto folder = fixtures.createFolder("Original");
    String originalName = folder.getName();
    
    // When — simulate an error during update
    UpdateFolderRequest request = new UpdateFolderRequest(
        folder.getId(), 
        "NewName", 
        UUID.randomUUID() // non-existent parent
    );
    
    // Then — verify rollback
    assertThatThrownBy(() -> updateFolderUseCase.execute(request))
        .isInstanceOf(ResourceNotFoundException.class);
    
    // Verify data has NOT changed
    FolderDto unchanged = getFolderUseCase.execute(folder.getId());
    assertThat(unchanged.getName()).isEqualTo(originalName);
}
```

**What to verify:**
- Automatic rollback on error.
- Data remains intact after rollback.
- Nested transactions managed correctly.

#### Queries and Filters
```java
@Test
void listFolders_should_returnOnlyRootFolders_when_parentIsNull() {
    // Given
    FolderDto parent = fixtures.createFolder("Parent");
    fixtures.createFolder("Child", parent.getId());
    fixtures.createFolder("Root1");
    fixtures.createFolder("Root2");
    
    // When — list only root folders
    List<FolderDto> roots = listFoldersUseCase.execute(null);
    
    // Then
    assertThat(roots).hasSize(3); // Parent, Root1, Root2
    assertThat(roots).allMatch(f -> f.getParentId() == null);
}
```

**What to verify:**
- Filters applied correctly.
- Pagination and sorting.
- Joins and fetches.
- Query performance (number of queries executed).

#### Side Effects and Cascades
```java
@Test
void deleteFolder_should_deleteChildren_when_cascadeEnabled() {
    // Given
    FolderDto parent = fixtures.createFolder("Parent");
    FolderDto child = fixtures.createFolder("Child", parent.getId());
    
    // When — delete the parent
    deleteFolderUseCase.execute(parent.getId());
    
    // Then — verify the child was also deleted
    assertThatThrownBy(() -> getFolderUseCase.execute(child.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
}
```

**What to verify:**
- Cascade delete working.
- Orphan removal.
- Soft delete (if implemented).
- Audit logs created correctly.

### Integration Test Best Practices
- **Autowire required beans**: use `@Autowired` to inject use cases, repositories, fixtures.
- **Reusable test fixtures**: create helper classes to generate test data (e.g. `TestFixtures`).
- **Automatically managed transactions**: no manual cleanup needed — Spring rolls back after each test.
- **Automatic Liquibase schema**: the schema is applied automatically on container startup.
- **Complete assertions**: verify both saving and retrieval of data from the database.
- **Realistic scenarios**: test real end-to-end use cases, not just isolated edge cases.
- **Descriptive names**: use the pattern `methodName_should_expectedBehavior_when_condition`.
- **Given-When-Then**: structure tests clearly (setup, action, assertion).
- **Test the final result**: do not stop at the return value — verify actual persistence in the database.

### Focus on What Matters

When writing an integration test, focus on:

1. **Verifiable persistence**: is the data actually in the database?
2. **Intact relations**: are foreign keys correct? Do relations navigate correctly?
3. **Respected constraints**: do unique constraints, not null, and check constraints work?
4. **Expected side effects**: cascade delete, audit logs, generated events?
5. **Transactionality**: on error, does a complete rollback occur?
6. **Correct business exceptions**: are the right exceptions thrown with the appropriate messages?

## Testing External Systems

### HTTP Integrations
- Use **WireMock** to simulate external APIs.
- **Never** call live systems during tests.
- **Configuration**: mock server started as part of the test.

### Best Practices
- Simulate both success and failure scenarios.
- Test timeout and retry logic.
- Validate headers, body, and status codes of requests.

## Maven Commands

### Run all integration tests
```bash
mvn test
```
Runs all integration tests (`*IT.java` files). Tests will load the full Spring context, start the PostgreSQL container via Testcontainers, apply Liquibase migrations, and run with a real database.

### Run a single integration test
```bash
mvn test -Dtest=CreateFolderUseCaseIT
```
Useful for quickly testing a single class without running the entire suite.

### Run tests with coverage report
```bash
mvn clean test
```
`clean` removes previous data, then `test` runs the tests and JaCoCo automatically generates the HTML report at `target/site/jacoco/index.html`.

### Run tests in verbose mode (debugging)
```bash
mvn test -X
```
Shows detailed output for debugging when tests fail. Useful for viewing Spring logs, SQL queries, and Testcontainers errors.

### Skip tests during build
```bash
mvn clean package -DskipTests
```
Useful for fast builds when tests are known to pass (e.g. deploying to a test environment).

## Code Coverage with JaCoCo

### Maven Configuration
The JaCoCo plugin is configured in `pom.xml` with two executions:

1. **prepare-agent**: activates the JaCoCo agent before test execution.
2. **report**: generates the HTML report after test execution.

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### How JaCoCo Works

1. **Instrumentation**: JaCoCo inserts bytecode into the compiled code to track which lines are executed.
2. **Execution**: during tests, JaCoCo records which lines of code are covered.
3. **Report**: after tests complete, it generates an HTML report with coverage metrics.

### Coverage Metrics

JaCoCo measures several types of coverage:

- **Line Coverage**: percentage of lines executed.
- **Branch Coverage**: percentage of branches (if/switch) executed.
- **Method Coverage**: percentage of methods invoked.
- **Class Coverage**: percentage of classes instantiated.
- **Complexity Coverage**: cyclomatic complexity coverage.

### Viewing the Report

After running `mvn test`, open:
```
target/site/jacoco/index.html
```

The report shows:
- **Overview**: global project coverage.
- **Package view**: drill-down by package.
- **Class view**: detail for each class.
- **Source view**: source code with green (covered) / red (not covered) highlighting.

### Colour Legend in the Report

- 🟢 **Green**: code covered by tests.
- 🔴 **Red**: code NOT covered by tests.
- 🟡 **Yellow**: branch partially covered (e.g. only the true branch of an if).

### Generated Files

- `target/jacoco.exec`: binary file with raw execution data.
- `target/site/jacoco/`: folder with HTML reports.
- `target/site/jacoco/jacoco.xml`: report in XML format (useful for CI/CD).

### Coverage Thresholds

Minimum coverage thresholds can be configured to fail the build:

```xml
<execution>
    <id>check</id>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

## General Best Practices

### FIRST Principles
- **Fast**: tests must be fast.
- **Independent**: each test must be independent.
- **Repeatable**: consistent results on every run.
- **Self-validating**: automatic pass/fail — no manual verification.
- **Timely**: write tests alongside the code (or before with TDD).

### Naming Convention
Method name pattern: `methodName_should_expectedBehavior_when_condition`

Examples:
- `createFolder_should_persistCorrectly_when_validRequest`
- `createFolder_should_throwException_when_parentNotFound`

### Assertion Library
Prefer **AssertJ** for fluent, readable assertions:
```java
assertThat(result).isNotNull();
assertThat(result.getName()).isEqualTo("Test");
assertThat(list).hasSize(3).contains(item1, item2);
assertThatThrownBy(() -> useCase.execute())
    .isInstanceOf(ResourceNotFoundException.class)
    .hasMessageContaining("not found");
```

### Test Data Management
- Use **Test Fixtures** for reusable data.
- Create builder patterns for complex objects.
- Do not hardcode test data — use factory methods.

### CI/CD Integration
- Tests must always pass before merging.
- CI pipeline must run `mvn clean test` on every commit.
- JaCoCo report must be published to monitor coverage over time.
- Consider quality gates based on minimum coverage (e.g. 80%).

## Troubleshooting

### Testcontainers does not start
- Verify that Docker is running.
- Check logs: `docker ps` and `docker logs`.
- Ensure sufficient disk space.

### Slow tests
- Verify the PostgreSQL container is in `reuse(true)` mode.
- Avoid loading too much data in fixtures.
- Use `@DirtiesContext` only when strictly necessary.

### Coverage not updating
- Run `mvn clean` before `mvn test`.
- Verify the JaCoCo plugin is in the correct lifecycle phase.
- Check that tests are actually running.
  - Testano use case completi con database reale
  - Caricano il contesto Spring Boot completo
  - Utilizzano Testcontainers per PostgreSQL

### Organizzazione delle Cartelle
```
src/test/java/
└── it/quix/nomecliente/
    └── domain/
        └── usecase/          # Integration tests per use case
            ├── BaseUseCaseIT.java  # Classe base condivisa
            └── *IT.java            # Test specifici per ogni use case
```
