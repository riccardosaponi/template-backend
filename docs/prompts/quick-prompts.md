# Quick Implementation Prompts - Common Patterns

These are ready-to-use prompt templates for common microservice scenarios. Copy, customize the placeholders, and use.

---

## 1. CRUD Entity with Database

**Use when**: You need basic Create/Read/Update/Delete operations for a domain entity with relational database persistence.

### Prompt
```
Implement CRUD REST API for [ENTITY_NAME] entity following hexagonal architecture.

Entity: [ENTITY_NAME] (e.g., "Product", "Customer", "Order")

Fields:
- id: UUID (PK)
- [field1]: [type] ([constraints])
- [field2]: [type] ([constraints])
- createdAt: Timestamp
- updatedAt: Timestamp

API Endpoints:
1. POST /api/v1/[entities] - Create new [entity]
2. GET /api/v1/[entities]/{id} - Get [entity] by ID
3. GET /api/v1/[entities] - List all [entities] (with pagination)
4. PUT /api/v1/[entities]/{id} - Update [entity]
5. DELETE /api/v1/[entities]/{id} - Delete [entity]

Business Rules:
[List validation rules, e.g.:]
- [field] is required
- [field] must be unique
- [field] max length 255
- [field] must match pattern [regex]

Authorization:
- All operations require authentication
- [Optional: specific role requirements per operation]

Database:
- Table: [table_name]
- Indexes: [list indexes]
- Constraints: [list FK, unique constraints]

Follow all project conventions in docs/ and implement complete test coverage.
```

---

## 2. Search/Filter API

**Use when**: You need to search or filter a collection with multiple criteria.

### Prompt
```
Implement search/filter API for [ENTITY_NAME] following hexagonal architecture.

Endpoint: GET /api/v1/[entities]/search

Query Parameters:
- [param1]: [type] - filter by [description]
- [param2]: [type] - filter by [description]
- sortBy: string - field to sort by
- sortDirection: ASC|DESC
- page: integer (default 0)
- size: integer (default 20)

Response:
- 200: Paginated list of [entities] matching filters
- 400: Invalid filter parameters

Business Rules:
[List filtering logic, e.g.:]
- If no filters: return all [entities]
- Filters are combined with AND logic
- Case-insensitive search on text fields
- Date range filtering supported

Database:
- Add indexes on filterable fields: [list fields]
- Use JPA Specification or JdbcTemplate with dynamic WHERE clause

Follow pagination conventions from docs/openapi-guidelines.md
```

---

## 3. File Upload with Metadata

**Use when**: You need to upload files and store them with metadata tracking.

### Prompt
```
Implement file upload API for [ENTITY_NAME] following hexagonal architecture.

Endpoint: POST /api/v1/[entities]/{entityId}/files

Request:
- Path param: entityId (UUID)
- Multipart: file (required)
- Optional: description, tags

Response:
- 201: File metadata (id, filename, size, contentType, storageKey)
- 400: Invalid file (empty, wrong type, too large)
- 404: Entity not found

Business Rules:
- Max file size: [SIZE] (e.g., 10MB)
- Allowed content types: [LIST] (e.g., image/*, application/pdf)
- Generate unique storage key: [FORMAT] (e.g., UUID + extension)
- Store metadata in database
- Store file content in [STORAGE] (filesystem, S3)

Entity: FileMetadata
- id: UUID
- entityId: UUID (FK to [entity])
- filename: String
- contentType: String
- size: Long (bytes)
- storageKey: String
- uploadedAt: Timestamp
- uploadedBy: String

Infrastructure:
- Create FileStoragePort interface in domain.port.out
- Implement FileSystemStorageAdapter in infrastructure
- Use temp directory for integration tests

Follow testing-strategy.md for file upload test patterns.
```

---

## 4. Aggregation/Statistics API

**Use when**: You need to compute and return aggregated data or statistics.

### Prompt
```
Implement statistics/aggregation API for [DOMAIN] following hexagonal architecture.

Endpoint: GET /api/v1/[domain]/statistics

Query Parameters (optional filters):
- dateFrom: date
- dateTo: date
- [otherFilters]

Response:
- 200: Statistics object with computed metrics

Statistics to compute:
[List metrics, e.g.:]
- total[Items]: count of all items
- [metric1]: sum/avg/max/min of [field]
- [metric2]: count grouped by [dimension]

Business Rules:
- Default date range: last 30 days
- Results cached for [DURATION] (e.g., 5 minutes)
- Only accessible by users with [ROLE]

Database:
- Create optimized query with aggregation functions (SUM, COUNT, AVG)
- Consider creating materialized view for complex calculations
- Add indexes on date fields used for filtering

Implementation:
- Use JdbcTemplate for native SQL queries with aggregations
- Return StatisticsDto from use case
- Document response schema in OpenAPI spec
```

---

## 5. Batch Processing API

**Use when**: You need to process multiple items in a single request.

### Prompt
```
Implement batch [OPERATION] API for [ENTITY_NAME] following hexagonal architecture.

Endpoint: POST /api/v1/[entities]/batch-[operation]

Request:
- Body: List of [operation]Requests
- Max batch size: [NUMBER] (e.g., 100)

Response:
- 200: BatchResult with success/failure per item
- 400: Batch size exceeded or validation error

BatchResult structure:
- totalItems: int
- successCount: int
- failureCount: int
- results: List<ItemResult>
  - itemId: identifier from request
  - status: SUCCESS | FAILURE
  - message: error message if failed
  - data: result data if success

Business Rules:
- Process all items even if some fail (partial success)
- Transaction per item OR single transaction (specify)
- Return detailed result for each item
- [Other business rules]

Implementation:
- Validate batch size limit
- Iterate through items in use case
- Catch and handle individual item failures
- Build comprehensive result object
- Consider async processing for large batches (optional)

Follow error-model.md for partial failure response format.
```

---

## 6. Entity Relationship Management

**Use when**: You need to manage relationships between two entities (e.g., add/remove items to collection).

### Prompt
```
Implement relationship management between [ENTITY_A] and [ENTITY_B] following hexagonal architecture.

Relationship: [ENTITY_A] has many [ENTITY_B] ([TYPE])
Type: ONE_TO_MANY | MANY_TO_MANY

API Endpoints:
1. POST /api/v1/[entityA]/{entityAId}/[entityB] - Add [entityB] to [entityA]
2. DELETE /api/v1/[entityA]/{entityAId}/[entityB]/{entityBId} - Remove relationship
3. GET /api/v1/[entityA]/{entityAId}/[entityB] - List all [entityB] for [entityA]

Business Rules:
- [EntityA] must exist (404 if not)
- [EntityB] must exist (404 if not)
- Prevent duplicate relationships (409 if already exists)
- [Other constraints]

Database:
[For MANY_TO_MANY:]
- Create junction table: [table_name]
  - entity_a_id: UUID (FK)
  - entity_b_id: UUID (FK)
  - PK: (entity_a_id, entity_b_id)

[For ONE_TO_MANY:]
- Add foreign key to [ENTITY_B] table: entity_a_id

Implementation:
- Create relationship-specific repository methods
- Implement cascade delete behavior (specify)
- Use transactional boundaries correctly
- Return appropriate DTOs with relationship data
```

---

## 7. State Machine / Workflow

**Use when**: An entity goes through different states with transition rules.

### Prompt
```
Implement state machine for [ENTITY_NAME] following hexagonal architecture.

Entity: [ENTITY_NAME]
Current state field: status (enum)

States:
- [STATE_1] - [description]
- [STATE_2] - [description]
- [STATE_3] - [description]

State Transitions:
- [STATE_1] → [STATE_2]: when [condition/action]
- [STATE_2] → [STATE_3]: when [condition/action]
- [STATE_2] → [STATE_1]: when [condition/action] (rollback)

API Endpoint:
POST /api/v1/[entities]/{id}/[action]

Actions:
- [action1]: triggers [STATE_X] → [STATE_Y]
- [action2]: triggers [STATE_Y] → [STATE_Z]

Business Rules:
- Only allowed transitions are permitted (400 for invalid transition)
- Transitions may require additional data (specify)
- Some transitions require specific role (e.g., ADMIN)
- Audit trail: log all state changes with timestamp and user

Implementation:
- Create Status enum in domain.ddd.enumeration
- Implement transition validation in use case
- Persist state change with audit fields (changedAt, changedBy)
- Create state_history table for audit trail
- Return updated entity with new state

Add transition validation tests for all state combinations.
```

---

## USAGE INSTRUCTIONS

1. **Choose the pattern** that matches your use case
2. **Copy the prompt template**
3. **Replace all [PLACEHOLDERS]** with your specific values
4. **Add or remove sections** as needed for your requirements
5. **Paste into AI agent** for implementation

All patterns automatically follow:
- ✅ Hexagonal architecture from `docs/architecture.md`
- ✅ Error handling from `docs/error-model.md`
- ✅ REST conventions from `docs/openapi-guidelines.md`
- ✅ Testing strategy from `docs/testing-strategy.md`
- ✅ Security model from `docs/security-keycloak.md`
- ✅ Definition of Done from `docs/dod.md`

---

## COMBINING PATTERNS

You can combine multiple patterns in a single prompt:

```
Implement [ENTITY] CRUD API (#1) with file upload capability (#3) and state machine (#7).

[Merge sections from patterns #1, #3, and #7]
[Specify how they integrate]
[Define additional business rules for combination]
```

---

## TIPS

- **Start simple**: Use pattern #1 (CRUD) as baseline, add complexity incrementally
- **Be explicit**: Even if pattern covers it, restate critical business rules
- **Reference specs**: Point to existing feature specs in `spec/features/` for similar patterns
- **Test coverage**: Always request unit + integration tests
- **Database indexes**: Specify indexes for performance-critical queries

