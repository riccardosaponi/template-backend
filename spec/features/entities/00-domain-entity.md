# Entity - Domain Model

## Overview
A **Entity** is a simple business entity characterized by:
- A unique **code** (alphanumeric identifier)
- A **description** (human-readable text)
- **Audit fields** (creation and update tracking)
- **Logical delete** via `canceled` flag

## Domain Model

### Entity: Entity

```yaml
Entity:
  id: UUID (PK)
  code: String(50) - unique, not null
  description: String(255) - not null
  createDate: Timestamp - not null
  createUser: String(128) - not null
  lastUpdateDate: Timestamp - nullable
  lastUpdateUser: String(128) - nullable
  canceled: Boolean - default false, not null
```

### Business Rules

1. **Code uniqueness**: Each entity must have a unique code
2. **Logical delete**: Deletion sets `canceled=true` instead of physical removal
3. **Audit trail**: All changes are tracked via audit fields
4. **Code immutability**: Once created, the code can be updated but should remain unique
5. **Trim validation**: Code and description are trimmed before persistence

### Use Cases

- **Create**: Create a new entity with code and description
- **Read**: Retrieve entity by ID
- **List**: List all entities with pagination and sorting
- **Update**: Modify code and/or description
- **Delete**: Logical delete (set canceled=true)

### Domain Events
None at this stage.

### Invariants
- Code must be unique across all non-canceled entities
- Canceled entities can still be retrieved but are excluded from default lists

