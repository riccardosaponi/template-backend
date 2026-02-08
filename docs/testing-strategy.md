# Testing Strategy

## Panoramica
La strategia di testing del progetto si basa su:
- **Integration tests**: testing end-to-end con database reale e contesto Spring completo
- **Code coverage**: monitoraggio automatico della copertura del codice tramite JaCoCo

## Struttura dei Test

### Nomenclatura dei File
- **Integration tests**: `*IT.java` (es. `CreateFolderUseCaseIT.java`)
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

## Integration Tests

### Caratteristiche
- **Contesto completo**: caricano Spring Boot con `@SpringBootTest`
- **Database reale**: PostgreSQL via Testcontainers
- **Migrazioni automatiche**: Liquibase applica lo schema da zero
- **Transaction management**: ogni test gira in una transazione (rollback automatico)

### Classe Base: `BaseUseCaseIT`
Tutti gli integration test ereditano da questa classe che fornisce:
- Container PostgreSQL condiviso (singleton pattern per performance)
- Configurazione automatica delle datasource properties
- Setup comune per tutti i test

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

### Esempio di Integration Test
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

### Perché Integration Test?

Gli integration test sono **fondamentali** perché verificano il comportamento **end-to-end** dell'applicazione in condizioni realistiche, simulando l'ambiente di produzione. A differenza dei test unitari che isolano singole unità di codice con mock, gli integration test validano l'intera catena di chiamate: dalla REST API, attraverso la business logic, fino alla persistenza su database reale.

#### 1. Validare l'Integrazione Reale dei Componenti
- **Database**: verifica che le query SQL/JPQL funzionino correttamente con un database reale (PostgreSQL)
- **Transazioni**: testa che le transazioni vengano gestite correttamente (commit/rollback, isolamento)
- **Spring Context**: garantisce che tutte le dipendenze siano iniettate correttamente e il wiring funzioni
- **Migrazioni**: valida che gli script Liquibase siano corretti, applicabili e coerenti con il codice
- **Configurazione**: verifica che le properties Spring siano correttamente configurate e applicate

#### 2. Scoprire Problemi Nascosti (che i mock non rilevano)
- **Mapping ORM**: errori di mapping tra entità JPA e tabelle database (annotazioni errate, type mismatch)
- **Vincoli database**: violazioni di unique constraint, foreign key, not null, check constraints
- **Type mismatch**: incompatibilità di tipi tra Java e SQL (es. UUID vs String, Timestamp vs LocalDateTime)
- **Lazy loading**: problemi di `LazyInitializationException` quando si accede a relazioni fuori dal contesto transazionale
- **N+1 queries**: individuare problemi di performance dovuti a query multiple non ottimizzate
- **Deadlock**: situazioni di deadlock database non rilevabili con mock

#### 3. Testare la Logica di Business Completa
- **Validazioni**: sia a livello applicativo (@Valid) che a livello database (constraints)
- **Business rules**: regole che coinvolgono più entità e operazioni coordinate
- **Side effects**: effetti collaterali su altre entità correlate (cascade, trigger, audit)
- **Workflow completi**: flussi che attraversano più use case (es. crea parent → crea child → verifica relazione)

#### 4. Garantire la Coerenza tra Spec e Implementazione
- **Schema database**: le tabelle esistono e hanno le colonne corrette
- **API contract**: il contratto OpenAPI è rispettato dall'implementazione
- **Data consistency**: i dati salvati rispettano le regole di business e i vincoli definiti

### Cosa Testare negli Integration Test

Gli integration test devono focalizzarsi sui **risultati finali osservabili** e sulla **persistenza effettiva dei dati**. Non basta verificare che un metodo non lanci eccezioni: bisogna validare che i dati siano stati effettivamente salvati, modificati o eliminati correttamente nel database, e che tutte le relazioni e i vincoli siano rispettati.

#### ✅ Persistenza e Recupero Dati

**Cosa è importante testare:**
- ✅ **ID generato automaticamente**: se il database genera l'ID, verificare che sia presente e non null
- ✅ **Tutti i campi persistiti correttamente**: ogni campo del DTO/Entity deve corrispondere al valore salvato
- ✅ **Timestamp automatici**: verificare che createdAt, updatedAt siano popolati automaticamente dal database
- ✅ **Valori di default**: colonne con default value devono avere il valore corretto anche se non specificato
- ✅ **Recupero effettivo dal database**: non fidarsi solo del valore di ritorno, ri-leggere dal DB per confermare la persistenza
```java
@Test
void createFolder_should_persistCorrectly_when_validRequest() {
    // Given
    CreateFolderRequest request = new CreateFolderRequest("Test Folder", null);
    
    // When - Persisti il dato
    FolderDto created = createFolderUseCase.execute(request);
    
    // Then - Verifica che il dato sia stato salvato correttamente
    assertThat(created.getId()).isNotNull();
    assertThat(created.getName()).isEqualTo("Test Folder");
    
    // Verifica recupero dal database
    FolderDto retrieved = getFolderUseCase.execute(created.getId());
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getName()).isEqualTo(created.getName());
}
```

**Cosa verificare:**
- ✅ ID generato automaticamente (se auto-increment)
- ✅ Campi popolati correttamente
- ✅ Timestamp automatici (createdAt, updatedAt)
- ✅ Valori di default applicati
- ✅ Dato effettivamente recuperabile dal database

#### ✅ Relazioni tra Entità (Parent-Child)
```java
@Test
void createFolder_should_linkToParent_when_parentExists() {
    // Given
    FolderDto parent = fixtures.createFolder("Parent");
    
    // When - Crea child con riferimento al parent
    CreateFolderRequest request = new CreateFolderRequest("Child", parent.getId());
    FolderDto child = createFolderUseCase.execute(request);
    
    // Then - Verifica la relazione
    assertThat(child.getParentId()).isEqualTo(parent.getId());
    
    // Verifica che il parent sia accessibile dalla query
    FolderDto retrieved = getFolderUseCase.execute(child.getId());
    assertThat(retrieved.getParentId()).isEqualTo(parent.getId());
}
```

**Cosa verificare:**
- ✅ Foreign key correttamente salvata
- ✅ Relazione bidirezionale (se presente)
- ✅ Cascade operations (se configurate)
- ✅ Navigazione tra entità correlate

#### ✅ Validazioni e Vincoli
```java
@Test
void createFolder_should_throwException_when_parentNotFound() {
    // Given
    UUID nonExistentId = UUID.randomUUID();
    CreateFolderRequest request = new CreateFolderRequest("Test", nonExistentId);
    
    // When/Then - Verifica che venga lanciata l'eccezione corretta
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
    
    // Then - Verifica normalizzazione (trim)
    assertThat(result.getName()).isEqualTo("Test Folder");
}
```

**Cosa verificare:**
- ✅ Eccezioni business corrette (ResourceNotFoundException, ValidationException)
- ✅ Unique constraint violations
- ✅ Not null violations
- ✅ Normalizzazione/sanitizzazione dati
- ✅ Validazioni custom (@Valid, @Validated)

#### ✅ Transazioni e Rollback
```java
@Test
void updateFolder_should_rollback_when_errorOccurs() {
    // Given
    FolderDto folder = fixtures.createFolder("Original");
    String originalName = folder.getName();
    
    // When - Simula un errore durante l'update
    UpdateFolderRequest request = new UpdateFolderRequest(
        folder.getId(), 
        "NewName", 
        UUID.randomUUID() // Parent inesistente
    );
    
    // Then - Verifica rollback
    assertThatThrownBy(() -> updateFolderUseCase.execute(request))
        .isInstanceOf(ResourceNotFoundException.class);
    
    // Verifica che i dati NON siano cambiati
    FolderDto unchanged = getFolderUseCase.execute(folder.getId());
    assertThat(unchanged.getName()).isEqualTo(originalName);
}
```

**Cosa verificare:**
- ✅ Rollback automatico in caso di errore
- ✅ Dati rimasti integri dopo il rollback
- ✅ Transazioni annidate gestite correttamente

#### ✅ Query e Filtri
```java
@Test
void listFolders_should_returnOnlyRootFolders_when_parentIsNull() {
    // Given
    FolderDto parent = fixtures.createFolder("Parent");
    fixtures.createFolder("Child", parent.getId());
    fixtures.createFolder("Root1");
    fixtures.createFolder("Root2");
    
    // When - Lista solo i root folders
    List<FolderDto> roots = listFoldersUseCase.execute(null);
    
    // Then
    assertThat(roots).hasSize(3); // Parent, Root1, Root2
    assertThat(roots).allMatch(f -> f.getParentId() == null);
}
```

**Cosa verificare:**
- ✅ Filtri applicati correttamente
- ✅ Paginazione e ordinamento
- ✅ Join e fetch corretti
- ✅ Performance query (numero di query eseguite)

#### ✅ Side Effects e Cascate
```java
@Test
void deleteFolder_should_deleteChildren_when_cascadeEnabled() {
    // Given
    FolderDto parent = fixtures.createFolder("Parent");
    FolderDto child = fixtures.createFolder("Child", parent.getId());
    
    // When - Elimina il parent
    deleteFolderUseCase.execute(parent.getId());
    
    // Then - Verifica che anche il child sia stato eliminato
    assertThatThrownBy(() -> getFolderUseCase.execute(child.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
}
```

**Cosa verificare:**
- ✅ Cascade delete funzionante
- ✅ Orphan removal
- ✅ Soft delete (se implementato)
- ✅ Audit log creati correttamente

### Best Practices per Integration Test
- **Autowire i bean necessari**: usa `@Autowired` per iniettare use case, repository, fixtures
- **Test fixtures riutilizzabili**: crea classi helper per generare dati di test (es. `TestFixtures`)
- **Transazioni gestite automaticamente**: non serve cleanup manuale, Spring gestisce il rollback dopo ogni test
- **Schema Liquibase automatico**: lo schema viene applicato automaticamente all'avvio del container
- **Assert completi**: verifica sia il salvataggio che il recupero dei dati dal database
- **Scenari realistici**: testa casi d'uso reali end-to-end, non solo edge case isolati
- **Nomi descrittivi**: usa il pattern `methodName_should_expectedBehavior_when_condition`
- **Given-When-Then**: struttura i test in modo chiaro (setup, azione, verifica)
- **Testa il risultato finale**: non fermarti al valore di ritorno, verifica la persistenza effettiva nel database

### Focus sui Risultati Importanti

Quando scrivi un integration test, concentrati su:

1. **Persistenza verificabile**: il dato è effettivamente nel database?
2. **Relazioni integre**: le foreign key sono corrette? Le relazioni navigano correttamente?
3. **Vincoli rispettati**: unique constraint, not null, check constraint funzionano?
4. **Side effects attesi**: cascade delete, audit log, eventi generati?
5. **Transazionalità**: in caso di errore, avviene il rollback completo?
6. **Eccezioni business corrette**: vengono lanciate le eccezioni giuste con i messaggi appropriati?

## Testing di Sistemi Esterni

### Integrazioni HTTP
- **WireMock** per simulare API esterne
- **MAI** chiamare sistemi live durante i test
- **Configurazione**: mock server avviato come parte del test

### Best Practices
- Simulare scenari di successo e fallimento
- Testare timeout e retry logic
- Validare headers, body e status code delle richieste

## Comandi Maven per gli Integration Test

### Eseguire tutti gli integration test
```bash
mvn test
```
Esegue tutti i test di integrazione (file `*IT.java`). I test caricheranno il contesto Spring completo, avvieranno il container PostgreSQL via Testcontainers, applicheranno le migrazioni Liquibase e eseguiranno i test con database reale.

### Eseguire un singolo integration test
```bash
mvn test -Dtest=CreateFolderUseCaseIT
```
Utile per testare rapidamente una singola classe senza eseguire tutta la suite.

### Eseguire i test con report di coverage
```bash
mvn clean test
```
Il comando `clean` rimuove i dati precedenti, poi `test` esegue i test e JaCoCo genera automaticamente il report HTML in `target/site/jacoco/index.html`.

### Eseguire i test in modalità verbose (debugging)
```bash
mvn test -X
```
Mostra output dettagliato per debugging quando i test falliscono. Utile per vedere log di Spring, SQL queries, errori di Testcontainers.

### Saltare i test durante il build
```bash
mvn clean package -DskipTests
```
Utile per build rapidi quando sai che i test passano (es. deploy in ambiente di test).

## Code Coverage con JaCoCo

### Configurazione Maven
Il plugin JaCoCo è configurato nel `pom.xml` con due execution:

1. **prepare-agent**: attiva l'agente JaCoCo prima dell'esecuzione dei test
2. **report**: genera il report HTML dopo l'esecuzione dei test

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

### Come Funziona JaCoCo

1. **Instrumentazione**: JaCoCo inserisce bytecode nel codice compilato per tracciare quali linee vengono eseguite
2. **Esecuzione**: durante i test, JaCoCo registra quali linee di codice vengono coperte
3. **Report**: al termine dei test, genera un report HTML con le metriche di coverage

### Metriche di Coverage

JaCoCo misura diversi tipi di coverage:

- **Line Coverage**: percentuale di linee eseguite
- **Branch Coverage**: percentuale di branch (if/switch) eseguiti
- **Method Coverage**: percentuale di metodi invocati
- **Class Coverage**: percentuale di classi istanziate
- **Complexity Coverage**: coverage della complessità ciclomatica

### Visualizzare il Report

Dopo aver eseguito `mvn test`, aprire:
```
target/site/jacoco/index.html
```

Il report mostra:
- **Overview**: coverage globale del progetto
- **Package view**: drill-down per package
- **Class view**: dettaglio per ogni classe
- **Source view**: codice sorgente con highlighting verde (coperto) / rosso (non coperto)

### Interpretazione dei Colori nel Report

- 🟢 **Verde**: codice coperto dai test
- 🔴 **Rosso**: codice NON coperto dai test
- 🟡 **Giallo**: branch parzialmente coperto (es. solo il ramo true di un if)

### File Generati

- `target/jacoco.exec`: file binario con i dati raw di execution
- `target/site/jacoco/`: cartella con i report HTML
- `target/site/jacoco/jacoco.xml`: report in formato XML (utile per CI/CD)

### Soglie di Coverage

È possibile configurare soglie minime di coverage che causano il fallimento del build:

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

### Script di Verifica Coverage

Lo script `check-coverage.sh` (se presente) può essere utilizzato per automatizzare il controllo della coverage:

```bash
./check-coverage.sh
```

## Best Practices Generali

### Principi FIRST
- **Fast**: i test devono essere veloci
- **Independent**: ogni test deve essere indipendente
- **Repeatable**: risultati consistenti ad ogni esecuzione
- **Self-validating**: pass/fail automatico, no verifica manuale
- **Timely**: scrivere i test insieme al codice (o prima con TDD)

### Naming Convention
- Nome del metodo descrittivo: `methodName_should_expectedBehavior_when_condition`
- Esempi:
  - `createFolder_should_persistCorrectly_when_validRequest`
  - `createFolder_should_throwException_when_parentNotFound`

### Assertion Library
- Preferire **AssertJ** per assertion fluenti e leggibili
- Esempi:
  ```java
  assertThat(result).isNotNull();
  assertThat(result.getName()).isEqualTo("Test");
  assertThat(list).hasSize(3).contains(item1, item2);
  assertThatThrownBy(() -> useCase.execute())
      .isInstanceOf(ResourceNotFoundException.class)
      .hasMessageContaining("not found");
  ```

### Test Data Management
- Utilizzare **Test Fixtures** per dati riutilizzabili
- Creare builder pattern per oggetti complessi
- Non hardcodare dati di test, usare factory methods

### CI/CD Integration
- I test devono passare sempre prima di fare merge
- Pipeline CI deve eseguire `mvn clean test` ad ogni commit
- Report JaCoCo deve essere pubblicato per monitorare la coverage nel tempo
- Considerare quality gate basati su coverage minima (es. 80%)

## Troubleshooting

### Testcontainers non parte
- Verificare che Docker sia in esecuzione
- Controllare i log: `docker ps` e `docker logs`
- Assicurarsi di avere spazio disco sufficiente

### Test lenti
- Verificare che il container PostgreSQL sia in modalità `reuse(true)`
- Evitare di caricare troppi dati nei fixtures
- Considerare l'uso di `@DirtiesContext` solo quando necessario

### Coverage non aggiornata
- Eseguire `mvn clean` prima di `mvn test`
- Verificare che il plugin JaCoCo sia nella giusta fase del lifecycle
- Controllare che i test stiano effettivamente girando

