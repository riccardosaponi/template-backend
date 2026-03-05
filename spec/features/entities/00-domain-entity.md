# Entity - Domain Model

## Overview
A **Entity** is a simple business entity characterized by:
- A unique **code** (alphanumeric identifier)
- A **description** (human-readable text)
- **Audit fields** (creation and update tracking)
- **Logical delete** via `canceled` flag

**Implementation**: See [`AGENTS.md`](../../../AGENTS.md), [`docs/architecture.md`](../../../docs/architecture.md), [`docs/dod.md`](../../../docs/dod.md).

---

## Domain Model

### Entity: Entity

```yaml
Entity (database table: entity)
  id: UUID (PK, entity_id)
  code: String(50) - unique, not null
  description: String(255) - not null
  created_by: String(255) - not null
  created_date: Timestamp - not null
  updated_by: String(255) - nullable
  updated_date: Timestamp - nullable
  canceled: int (0/1) - default 0, not null
  delete_date: Timestamp - nullable
  delete_user: String(100) - nullable
```

### Business Rules

1. **Code uniqueness**: Each entity must have a unique code (across non-canceled records)
2. **Logical delete**: Deletion sets `canceled=1` (never physical delete)
3. **Audit trail**: All changes tracked via `created_by`, `created_date`, `updated_by`, `updated_date`
4. **Code immutability**: Code can be updated but must remain unique
5. **Trim validation**: Code and description trimmed before persistence

### Use Cases

- **Create**: Create a new entity with code and description
- **Read**: Retrieve entity by ID
- **List**: List all entities with pagination and sorting
- **Update**: Modify code and/or description
- **Delete**: Logical delete (set `canceled=1`, populate `delete_date` + `delete_user`)

### Domain Events
None at this stage.

### Invariants
- Code must be unique across all non-canceled entities
- Canceled entities can be retrieved by ID but excluded from default lists

