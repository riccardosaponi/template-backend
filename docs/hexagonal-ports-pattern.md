# Hexagonal Architecture - IN Ports Pattern

## Overview
This document defines the standard pattern for implementing IN ports in REST adapters following hexagonal architecture principles.

## Core Principle
**Every REST adapter MUST implement its corresponding IN ports.**

This ensures:
- ✅ Clear separation between HTTP layer and business logic
- ✅ Testability (ports can be mocked)
- ✅ Adherence to hexagonal architecture
- ✅ Contract-first approach

## Pattern

### 1. Define IN Port Interface
One interface per business operation in `domain.port.in`:

```java
package it.quix.nomecliente.domain.port.in;

import it.quix.nomecliente.domain.ddd.dto.CreateFolderRequest;
import it.quix.nomecliente.domain.ddd.dto.FolderDto;
import org.springframework.http.ResponseEntity;

/**
 * IN port for creating a folder.
 */
public interface CreateFolderPortIn {
    
    /**
     * Create a new folder.
     * 
     * @param request the folder creation request
     * @return HTTP 201 with the created folder
     */
    ResponseEntity<FolderDto> createFolder(CreateFolderRequest request);
}
```

### 2. REST Adapter Implements IN Port

```java
package it.quix.nomecliente.application;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderRestAdapter implements CreateFolderPortIn {
    
    private final CreateFolderUseCase createFolderUseCase;
    
    @PostMapping
    @Override  // ← MANDATORY: implements IN port
    @Operation(summary = "Create a new folder")
    public ResponseEntity<FolderDto> createFolder(
        @Valid @RequestBody CreateFolderRequest request
    ) {
        FolderDto created = createFolderUseCase.execute(request);
        return ResponseEntity
            .created(URI.create("/api/v1/folders/" + created.getId()))
            .body(created);
    }
}
```

### 3. Use Case Executes Business Logic

```java
package it.quix.nomecliente.domain.usecase;

@Service
@RequiredArgsConstructor
public class CreateFolderUseCaseImpl implements CreateFolderUseCase {
    
    private final FolderRepositoryPort folderRepository;
    
    @Override
    public FolderDto execute(CreateFolderRequest request) {
        // Business logic here
        Folder folder = Folder.builder()
            .name(request.getName())
            .aclMode(request.getAclMode())
            .build();
            
        Folder saved = folderRepository.save(folder);
        return FolderDto.from(saved);
    }
}
```

## Best Practices

### ✅ DO

1. **Match signatures**: IN port method signature MUST match REST adapter method
   ```java
   // IN Port
   ResponseEntity<FolderDto> createFolder(CreateFolderRequest request);
   
   // REST Adapter - SAME signature
   @PostMapping
   @Override
   public ResponseEntity<FolderDto> createFolder(@Valid @RequestBody CreateFolderRequest request)
   ```

2. **Use @Override**: Always annotate REST methods with `@Override`
   ```java
   @GetMapping("/{id}")
   @Override  // ← confirms implementation of IN port
   public FolderDto getFolder(@PathVariable UUID id)
   ```

3. **HTTP status in ports**: For non-200 responses, use `ResponseEntity<T>`
   ```java
   // For HTTP 201 Created
   ResponseEntity<FolderDto> createFolder(...);
   
   // For HTTP 200 OK
   FolderDto getFolder(UUID id);
   
   // For HTTP 204 No Content
   void deleteFolder(UUID id, boolean recursive);
   ```

4. **REST parameters in ports**: Use individual parameters, not framework types
   ```java
   // ✅ GOOD - REST parameters
   Page<FolderDto> listFolders(UUID parentId, int page, int size, String sortBy, String sortDirection);
   
   // ❌ BAD - Spring framework type in port
   Page<FolderDto> listFolders(UUID parentId, Pageable pageable);
   ```
   
   The REST adapter converts REST params → framework types internally:
   ```java
   @Override
   public Page<FolderDto> listFolders(UUID parentId, int page, int size, String sortBy, String sortDirection) {
       Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
       return listFoldersUseCase.execute(parentId, pageable);
   }
   ```

5. **One port per operation**: Keep ports focused and cohesive
   ```java
   // ✅ GOOD
   CreateFolderPortIn
   GetFolderPortIn
   UpdateFolderPortIn
   DeleteFolderPortIn
   
   // ❌ BAD
   FolderPortIn (with all methods)
   ```

### ❌ DON'T

1. **Don't skip IN ports**: REST adapters must always implement ports
   ```java
   // ❌ BAD
   @RestController
   public class FolderRestAdapter {  // no implements!
   ```

2. **Don't put business logic in REST adapter**: Keep it thin
   ```java
   // ❌ BAD - business logic in adapter
   @PostMapping
   public ResponseEntity<FolderDto> createFolder(...) {
       if (request.getName().length() < 3) {  // ← validation logic!
           throw new ValidationException();
       }
       // more business logic...
   }
   
   // ✅ GOOD - delegate to use case
   @PostMapping
   @Override
   public ResponseEntity<FolderDto> createFolder(...) {
       FolderDto created = createFolderUseCase.execute(request);  // ← delegate
       return ResponseEntity.created(...).body(created);
   }
   ```

3. **Don't mix HTTP concerns in ports**: Keep ports technology-agnostic
   ```java
   // ❌ BAD - HTTP annotations in port
   public interface CreateFolderPortIn {
       @PostMapping  // ← NO!
       ResponseEntity<FolderDto> createFolder(@RequestBody CreateFolderRequest request);
   }
   
   // ✅ GOOD - clean interface
   public interface CreateFolderPortIn {
       ResponseEntity<FolderDto> createFolder(CreateFolderRequest request);
   }
   ```

## Flow Diagram

```
REST Request
     ↓
[REST Adapter] ← implements → [IN Port]
     ↓ delegates
[Use Case Impl] ← implements → [Use Case]
     ↓ uses
[OUT Port] ← implemented by → [Repository/Adapter]
     ↓
Database/External Service
```

## Checklist

When implementing a new REST endpoint:

- [ ] Define IN port interface in `domain.port.in`
- [ ] REST adapter implements the IN port
- [ ] All public REST methods have `@Override`
- [ ] Method signatures match exactly (parameters, return type)
- [ ] REST adapter delegates to use case (thin layer)
- [ ] Use case contains business logic
- [ ] Use case calls OUT ports for persistence/external services

## Example: Complete CRUD

```java
// 1. IN Ports
public interface CreateFolderPortIn {
    ResponseEntity<FolderDto> createFolder(CreateFolderRequest request);
}

public interface GetFolderPortIn {
    FolderDto getFolder(UUID id);
}

public interface ListFoldersPortIn {
    Page<FolderDto> listFolders(UUID parentId, int page, int size, String sortBy, String sortDirection);
}

public interface UpdateFolderPortIn {
    FolderDto updateFolder(UUID id, UpdateFolderRequest request);
}

public interface DeleteFolderPortIn {
    void deleteFolder(UUID id, boolean recursive);
}

// 2. REST Adapter
@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderRestAdapter implements
    CreateFolderPortIn,
    GetFolderPortIn,
    ListFoldersPortIn,
    UpdateFolderPortIn,
    DeleteFolderPortIn {
    
    private final CreateFolderUseCase createFolderUseCase;
    private final GetFolderUseCase getFolderUseCase;
    // ... other use cases
    
    @PostMapping
    @Override
    public ResponseEntity<FolderDto> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        FolderDto created = createFolderUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/folders/" + created.getId())).body(created);
    }
    
    @GetMapping("/{id}")
    @Override
    public FolderDto getFolder(@PathVariable UUID id) {
        return getFolderUseCase.execute(id);
    }
    
    @GetMapping
    @Override
    public Page<FolderDto> listFolders(
        @RequestParam(required = false) UUID parentId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        return listFoldersUseCase.execute(parentId, pageable);
    }
    
    @PutMapping("/{id}")
    @Override
    public FolderDto updateFolder(@PathVariable UUID id, @Valid @RequestBody UpdateFolderRequest request) {
        return updateFolderUseCase.execute(id, request);
    }
    
    @DeleteMapping("/{id}")
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFolder(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean recursive) {
        deleteFolderUseCase.execute(id, recursive);
    }
}
```

## Benefits

1. **Testability**: IN ports enable integration testing with real Spring context and Testcontainers
2. **Clear contracts**: Ports define explicit API contracts
3. **Technology independence**: Domain doesn't know about Spring/HTTP
4. **Maintainability**: Changes to REST layer don't affect domain
5. **Documentation**: Ports serve as living documentation

## References
- Hexagonal Architecture (Alistair Cockburn)
- Clean Architecture (Robert C. Martin)
- Port and Adapters Pattern

