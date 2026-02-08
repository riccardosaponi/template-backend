# Error Model - Centralized Error Handling

All error responses follow a consistent structure defined by `ErrorResponse` DTO.

## Implementation

**Classes**:
- `ErrorResponse` - DTO for error responses
- `ErrorDetail` - DTO for field-specific error details
- `GlobalExceptionHandler` - Centralized exception handler (`@RestControllerAdvice`)

**Location**: `it.quix.nomecliente.domain.ddd.dto` and `it.quix.nomecliente.config`

---

## JSON Structure

```json
{
  "code": "STRING_STABLE_CODE",
  "message": "Human readable message",
  "details": [
    { "field": "optional", "issue": "optional" }
  ],
  "correlationId": "optional"
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | String | ✅ Yes | Stable error code for client handling |
| `message` | String | ✅ Yes | Human-readable error message |
| `details` | Array | ❌ Optional | Field-specific error details (validation, conflicts) |
| `correlationId` | String | ❌ Optional | Unique ID for error tracking and support |

---

## Error Codes

| Code | HTTP Status | Description | Example |
|------|-------------|-------------|---------|
| `RESOURCE_NOT_FOUND` | 404 | Resource not found | Entity with ID not found |
| `VALIDATION_ERROR` | 400 | Request validation failed | Invalid field values |
| `BUSINESS_RULE_VIOLATION` | 400/409 | Business rule violated | Unique constraint |
| `FORBIDDEN` | 403 | Insufficient permissions | User lacks required role |
| `UNAUTHORIZED` | 401 | Authentication required | Missing or invalid JWT |
| `INVALID_ARGUMENT` | 400 | Invalid argument | Illegal parameter value |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected error | Generic server error |

---

## Examples

### 404 Not Found

```http
GET /api/v1/entities/123e4567-e89b-12d3-a456-426614174000
```

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Entity not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 400 Validation Error

```http
POST /api/v1/entities
{
  "code": "",
  "description": ""
}
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "code",
      "issue": "must not be blank"
    },
    {
      "field": "description",
      "issue": "must not be blank"
    }
  ],
  "correlationId": "b2c3d4e5-f6g7-8901-bcde-f12345678901"
}
```

### 403 Forbidden

```http
DELETE /api/v1/admin/entities
Authorization: Bearer <user-token>
```

```json
{
  "code": "FORBIDDEN",
  "message": "Access denied: insufficient permissions",
  "correlationId": "c3d4e5f6-g7h8-9012-cdef-g23456789012"
}
```

### 401 Unauthorized

```http
GET /api/v1/entities
```

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "correlationId": "d4e5f6g7-h8i9-0123-defg-h34567890123"
}
```

### 409 Conflict (Business Rule)

```http
POST /api/v1/entities
{
  "code": "EXISTING-CODE",
  "description": "Test"
}
```

```json
{
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Entity with code EXISTING-CODE already exists (conflict)",
  "correlationId": "e5f6g7h8-i9j0-1234-efgh-i45678901234"
}
```

### 500 Internal Server Error

```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "An unexpected error occurred. Please contact support with correlation ID: f6g7h8i9-j0k1-2345-fghi-j56789012345",
  "correlationId": "f6g7h8i9-j0k1-2345-fghi-j56789012345"
}
```

---

## Principles

### ✅ DO

1. **Use stable codes**: Clients rely on `code` for error handling
2. **Provide correlation IDs**: Essential for debugging and support
3. **Be specific**: Include helpful details without leaking sensitive info
4. **Log with correlation ID**: Link errors in logs to user reports

### ❌ DON'T

1. **Don't leak internals**: No stack traces, SQL queries, or credentials
2. **Don't use generic messages**: "Error occurred" is not helpful
3. **Don't change codes**: Breaking change for clients
4. **Don't expose database details**: Abstract technical errors

---

## Exception Mapping

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `BusinessRuleViolationException` | 400/409 | `BUSINESS_RULE_VIOLATION` |
| `ForbiddenException` | 403 | `FORBIDDEN` |
| `AccessDeniedException` (Spring Security) | 403 | `FORBIDDEN` |
| `AuthenticationException` (Spring Security) | 401 | `UNAUTHORIZED` |
| `IllegalArgumentException` | 400 | `INVALID_ARGUMENT` |
| `Exception` (generic) | 500 | `INTERNAL_SERVER_ERROR` |

---

## Usage in Code

### Throw Exception

```java
@Service
public class GetEntityUseCaseImpl implements GetEntityUseCase {
    
    @Override
    public EntityDto execute(UUID id) {
        Entity entity = entityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Entity not found with id: " + id
            ));
        
        return mapToDto(entity);
    }
}
```

### Custom Exception

```java
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

### Global Handler (automatic)

The `GlobalExceptionHandler` automatically catches all exceptions and converts them to `ErrorResponse`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("Resource not found [correlationId={}]: {}", correlationId, ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

---

## Logging

All errors are logged with correlation ID:

```
WARN  Resource not found [correlationId=a1b2c3d4-e5f6-7890-abcd-ef1234567890]: Entity not found with id: 123e4567-e89b-12d3-a456-426614174000
ERROR Unexpected error [correlationId=f6g7h8i9-j0k1-2345-fghi-j56789012345]: Database connection timeout
```

Users can provide the correlation ID to support for faster issue resolution.

---

## Testing

Mock error responses in integration tests:

```java
@Test
void shouldReturn404_whenEntityNotFound() {
    UUID nonExistentId = UUID.randomUUID();
    
    assertThatThrownBy(() -> getEntityUseCase.execute(nonExistentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Entity not found with id: " + nonExistentId);
}
```

The `GlobalExceptionHandler` will automatically convert this to a proper `ErrorResponse` with HTTP 404.

---

## ✅ Implementation Complete

- [x] `ErrorResponse` DTO created
- [x] `ErrorDetail` DTO created
- [x] `GlobalExceptionHandler` with @RestControllerAdvice
- [x] All standard exceptions handled
- [x] Correlation IDs generated
- [x] Logging with correlation IDs
- [x] Security exceptions (401/403) handled
- [x] Validation errors with field details
- [x] Tests passing

**🎯 Centralized error handling production-ready!**

