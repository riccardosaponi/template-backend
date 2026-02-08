# Specs

This folder contains feature specifications for end-to-end development.

## Unified CRUD Specifications

Each feature has a **comprehensive CRUD specification** that covers the complete lifecycle from API to database.

### Why Unified Specs?

Instead of fragmenting specs into multiple files (domain, create-api, update-api, etc.), we use **single comprehensive documents** that include:
- ✅ Domain model and business rules
- ✅ All CRUD operations (Create, Read, Update, Delete)
- ✅ API contracts (endpoints, request/response)
- ✅ Authorization requirements
- ✅ Database schema
- ✅ Error handling
- ✅ Acceptance criteria (Given/When/Then)
- ✅ Testing requirements

**Benefits**:
- Complete context in one place
- Perfect for AI-assisted implementation (single prompt)
- Easy to understand full feature scope
- Reduces context switching
- Developer implements entire feature end-to-end

### Current Features

#### 📁 Folders
**Spec**: `features/folders/folders-crud.md`

Complete CRUD operations for folder management:
- Create folder (with parent hierarchy)
- Get folder by ID
- List folders (with pagination, filters)
- Update folder (name, ACL mode)
- Delete folder (with recursive option)

Includes hierarchical structure, ACL inheritance, and permission checks.

#### 📄 Documents
**Spec**: `features/documents/documents-crud.md`

Complete CRUD operations for document management:
- Upload document (multipart file + metadata)
- Get document metadata
- List documents in folder (with pagination, search)
- Download document content
- Delete document

Includes file storage integration, content type validation, and size limits.

#### 🔐 Permissions
**Spec**: `features/permissions/00-domain-permissions.md`

Permission model and ACL rules:
- Roles: VIEWER, COLLABORATOR, ADMIN
- Subject types: USER, GROUP
- ACL modes: INHERIT, OVERRIDE
- Permission resolution algorithm
- Authorization patterns

Referenced by both folders and documents.

---

## Using the Specs

### For Implementation

**Single Prompt Approach** (recommended):

```
Implement complete CRUD for [FEATURE] following spec/features/[feature]/[feature]-crud.md

Include:
- All 5 CRUD operations (Create, Read, List, Update, Delete)
- REST adapters following docs/architecture.md
- Use cases with business logic
- Repository implementations
- Liquibase changeset
- Unit + integration tests

Follow all project conventions in docs/
```

**Why this works**:
1. Spec contains complete business logic
2. `docs/` contains architectural conventions
3. AI combines both for complete implementation
4. Single workflow from API to database

### For Review

When reviewing implementations:
- ✅ All operations from spec implemented?
- ✅ All acceptance criteria covered?
- ✅ All error cases handled?
- ✅ Tests include all scenarios?
- ✅ Database schema matches spec?

---

## Spec Structure

Each unified CRUD spec follows this format:

### 1. Domain Model
- Entity fields with types
- Business rules and invariants
- Relationships with other entities

### 2. API Operations
For each CRUD operation:
- **Endpoint**: HTTP method and path
- **Input**: request body/parameters
- **Output**: response structure and status codes
- **Business Rules**: validation, authorization, logic
- **Application Flow**: step-by-step process
- **Error Handling**: all error scenarios
- **Acceptance Criteria**: Given/When/Then scenarios

### 3. Database Schema
- Table structure with columns
- Indexes for performance
- Constraints (PK, FK, unique, check)

### 4. Testing Requirements
- Unit test coverage
- Integration test scenarios
- Test data setup

### 5. Notes
- Performance considerations
- Security considerations
- Future enhancements
- Related features

---

## Legacy Specs

Older fragmented specs still exist but are deprecated:
- `folders/00-domain-folder.md` → superseded by `folders/folders-crud.md`
- `folders/10-api-create-folder.md` → included in `folders/folders-crud.md`
- `documents/00-domain-document.md` → superseded by `documents/documents-crud.md`
- `documents/10-api-upload-document.md` → included in `documents/documents-crud.md`

**Use the unified `-crud.md` specs** for new development.

---

## Contract

OpenAPI specification: `api/openapi.yaml`

This is the **source of truth** for:
- Endpoint paths and HTTP methods
- Request/response schemas
- Status codes
- Operation IDs
- API documentation

The spec files provide business context and acceptance criteria.
The OpenAPI file provides the technical contract.

---

## Adding New Features

When adding a new feature:

1. **Create unified spec**: `features/[feature]/[feature]-crud.md`
2. **Follow template structure**: domain model → operations → schema → tests
3. **Include all CRUD operations**: even if not all implemented initially
4. **Add acceptance criteria**: Given/When/Then for each operation
5. **Reference related features**: link to other specs if needed
6. **Update this README**: add feature to "Current Features" section

---

## Tips

### Writing Good Specs
- ✅ Focus on **business logic**, not implementation details
- ✅ Use clear **acceptance criteria** (Given/When/Then)
- ✅ Include **all error scenarios** with status codes
- ✅ Specify **authorization requirements** explicitly
- ✅ Keep it **concise** but complete
- ❌ Don't specify class names or package structure (that's in `docs/architecture.md`)
- ❌ Don't repeat architectural patterns (that's in `docs/`)

### Using Specs for Implementation
- Read spec completely before starting
- Implement all operations together (better context)
- Reference `docs/` for "how" (architecture, patterns)
- Reference spec for "what" (business rules, acceptance)
- Use spec acceptance criteria as test cases

### Maintaining Specs
- Update spec when business rules change
- Keep acceptance criteria aligned with implementation
- Add new scenarios as edge cases discovered
- Archive deprecated specs (don't delete, for history)

---

## Questions?

- **Spec unclear?** Open an issue or update spec with clarification
- **Missing scenario?** Add acceptance criteria to spec
- **Implementation question?** Check `docs/architecture.md` for patterns
- **Need example?** Look at existing `-crud.md` specs
