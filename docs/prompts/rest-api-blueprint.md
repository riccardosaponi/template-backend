# REST API Feature Implementation - Blueprint Prompt

Use this blueprint to implement a complete REST API feature following the hexagonal architecture.

---

## PROMPT TEMPLATE

```
Implement the following REST API feature in the Spring Boot project following hexagonal architecture guidelines.

### Feature: [FEATURE_NAME]
Example: "User Profile Management"

### Business Requirements
[Describe the business functionality clearly]

Example:
- Allow users to view their profile information
- Allow users to update their email and display name
- Validate email format before saving
- Return 404 if profile not found
- Return 403 if user tries to access another user's profile

### API Contract
Reference: `api/openapi.yaml` operationId: `[OPERATION_ID]`

**Endpoint**: [METHOD] [PATH]
Example: GET /api/v1/users/{userId}/profile

**Request**:
[Describe input parameters, body structure, headers]

Example:
- Path parameter: userId (UUID)
- Headers: Authorization (Bearer token)

**Response**:
[Describe successful and error responses]

Example:
- 200: ProfileDto with userId, email, displayName, createdAt
- 403: User not authorized to view this profile
- 404: Profile not found

### Business Rules
[List specific business logic and validation rules]

Example:
1. Email must be unique across all users
2. Display name is required, max 100 characters
3. Only the profile owner can update their own profile
4. Email format must be validated (RFC 5322 compliant)
5. Audit trail: track who updated what and when

### Data Model
[Describe entities and DTOs needed]

Example:
Entity: UserProfile
- id: UUID
- userId: UUID (matches Keycloak user ID)
- email: String
- displayName: String
- createdAt: Timestamp
- updatedAt: Timestamp
- updatedBy: String

### Database Schema
[Describe tables, columns, constraints, indexes]

Example:
Table: user_profiles
- id (UUID, PK)
- user_id (UUID, NOT NULL, UNIQUE)
- email (VARCHAR 255, NOT NULL, UNIQUE)
- display_name (VARCHAR 100, NOT NULL)
- created_at (TIMESTAMP, DEFAULT NOW())
- updated_at (TIMESTAMP)
- updated_by (VARCHAR 255)

Indexes:
- idx_user_profiles_user_id on user_id
- uk_user_profiles_email on email (unique)

### Acceptance Criteria
[Given/When/Then scenarios]

Example:
Scenario 1: Successfully retrieve profile
- Given a user profile exists for userId "abc-123"
- When calling GET /api/v1/users/abc-123/profile
- Then response is 200 with complete profile data

Scenario 2: Profile not found
- Given no profile exists for userId "xyz-999"
- When calling GET /api/v1/users/xyz-999/profile
- Then response is 404 with error details

Scenario 3: Update profile successfully
- Given a user profile exists
- When calling PUT with valid email and displayName
- Then response is 200 with updated profile
- And database reflects changes with updated timestamp

### Testing Requirements
[Specify test coverage expectations]

Example:
Unit Tests:
- Use case business logic (validation, authorization)
- Email format validation
- Display name length validation

Integration Tests:
- Full endpoint testing with Testcontainers + PostgreSQL
- Test all HTTP status codes (200, 400, 403, 404)
- Verify database persistence
- Test concurrent updates (optimistic locking if needed)

---

Implementation checklist:
□ Update api/openapi.yaml with endpoint definition
□ Create Liquibase changeset for database schema
□ Create domain entities and DTOs
□ Create IN port interface
□ Create use case interface and implementation with business logic
□ Create OUT port interface for repository
□ Create JPA/JdbcTemplate repository implementation in infrastructure
□ Create REST adapter in application layer
□ Write unit tests for use case
□ Write integration tests for endpoint + database
□ Update AGENTS.md examples if this represents a common pattern
```

---

## INSTRUCTIONS FOR USE

1. **Copy the template above**
2. **Replace placeholders** in [BRACKETS] with your specific values
3. **Fill in all sections** with concrete details from your feature specification
4. **Remove example text** and keep only your actual requirements
5. **Submit the filled prompt** to the AI agent for implementation

The agent will:
- Follow hexagonal architecture patterns from `docs/architecture.md`
- Implement proper error handling per `docs/error-model.md`
- Follow REST API conventions from `docs/openapi-guidelines.md`
- Create comprehensive tests per `docs/testing-strategy.md`
- Apply Liquibase best practices from `docs/liquibase-guidelines.md`
- Use proper security patterns from `docs/security-keycloak.md`
- Meet Definition of Done criteria from `docs/dod.md`

---

## TIPS FOR EFFECTIVE PROMPTS

### Focus on Business Logic
✅ DO: "Email must be unique. Validation should return 400 with specific error message."
❌ DON'T: "Create EmailValidator class in util package."

### Be Specific About Edge Cases
✅ DO: "If email is already taken by another user, return 409 Conflict with error code EMAIL_ALREADY_EXISTS"
❌ DON'T: "Handle duplicate emails somehow"

### Define Clear Acceptance Criteria
✅ DO: "Given user X tries to update user Y's profile, when authorization check runs, then return 403"
❌ DON'T: "Make sure users can't update other profiles"

### Specify Test Expectations
✅ DO: "Integration test must verify database rollback on validation failure"
❌ DON'T: "Write some tests"

### Include All HTTP Status Codes
✅ DO: List all expected responses: 200, 400, 401, 403, 404, 409, 500
❌ DON'T: Only mention success case

---

## COMMON PATTERNS

### Read-Only API (GET)
- Usually needs: repository query port
- Focus on: filtering, pagination, sorting
- Authorization: check read permissions

### Create API (POST)
- Usually needs: repository save port, validation logic
- Focus on: input validation, uniqueness checks, default values
- Authorization: check create permissions
- Response: 201 with Location header

### Update API (PUT/PATCH)
- Usually needs: repository find + update ports
- Focus on: existence check, validation, optimistic locking
- Authorization: check update permissions
- Response: 200 with updated resource

### Delete API (DELETE)
- Usually needs: repository find + delete ports
- Focus on: existence check, cascade logic, soft delete
- Authorization: check delete permissions
- Response: 204 No Content

### File Upload API (POST multipart)
- Usually needs: repository port, file storage port
- Focus on: file validation (size, type), storage key generation
- Authorization: check upload permissions
- Response: 201 with file metadata

---

## VALIDATION CHECKLIST

Before submitting your prompt, verify:
- [ ] Business requirements are clear and complete
- [ ] All API contract details specified (method, path, request, response)
- [ ] Business rules explicitly listed
- [ ] Data model includes all fields with types
- [ ] Database schema defines tables, columns, constraints, indexes
- [ ] Acceptance criteria cover happy path + error cases
- [ ] Testing requirements include unit + integration tests
- [ ] Authorization requirements specified
- [ ] Error responses and status codes defined
- [ ] Edge cases and validations documented

