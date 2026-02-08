# Entity CRUD - API Specification

## Endpoints

### 1. Create Entity
**POST** `/api/v1/entities`

**Request Body:**
```json
{
  "code": "ENT001",
  "description": "First entity"
}
```

**Response:** `201 Created`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "code": "ENT001",
  "description": "First entity",
  "createDate": "2024-02-08T10:15:30.123Z",
  "createUser": "system",
  "lastUpdateDate": null,
  "lastUpdateUser": null,
  "canceled": false
}
```

**Location Header:** `/api/v1/entities/{id}`

**Error Responses:**
- `400 Bad Request`: Validation error (code/description blank, too long, etc.)
- `409 Conflict`: Code already exists

---

### 2. Get Entity by ID
**GET** `/api/v1/entities/{id}`

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "code": "ENT001",
  "description": "First entity",
  "createDate": "2024-02-08T10:15:30.123Z",
  "createUser": "system",
  "lastUpdateDate": null,
  "lastUpdateUser": null,
  "canceled": false
}
```

**Error Responses:**
- `404 Not Found`: Entity not found

---

### 3. List Entities
**GET** `/api/v1/entities?page=0&size=20&sortBy=code&sortDirection=asc`

**Query Parameters:**
- `page`: Page number (0-based), default 0
- `size`: Page size, default 20
- `sortBy`: Field to sort by (code, description, createDate), default "code"
- `sortDirection`: asc or desc, default "asc"

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "code": "ENT001",
      "description": "First entity",
      "createDate": "2024-02-08T10:15:30.123Z",
      "createUser": "system",
      "lastUpdateDate": null,
      "lastUpdateUser": null,
      "canceled": false
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 4. Update Entity
**PUT** `/api/v1/entities/{id}`

**Request Body:**
```json
{
  "code": "ENT001-UPDATED",
  "description": "Updated description"
}
```

**Response:** `200 OK`
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "code": "ENT001-UPDATED",
  "description": "Updated description",
  "createDate": "2024-02-08T10:15:30.123Z",
  "createUser": "system",
  "lastUpdateDate": "2024-02-08T11:20:45.678Z",
  "lastUpdateUser": "system",
  "canceled": false
}
```

**Error Responses:**
- `400 Bad Request`: Validation error
- `404 Not Found`: Entity not found
- `409 Conflict`: Code already exists (if changed)

---

### 5. Delete Entity (Logical)
**DELETE** `/api/v1/entities/{id}`

**Response:** `204 No Content`

**Note:** This is a logical delete. The entity is marked as `canceled=true` but remains in the database.

**Error Responses:**
- `404 Not Found`: Entity not found

---

## Business Logic

### Validation Rules
- **Code**: 
  - Required, not blank
  - Max length: 50 characters
  - Must be unique
  - Trimmed before persistence
- **Description**:
  - Required, not blank
  - Max length: 255 characters
  - Trimmed before persistence

### Audit Fields
- **createDate**: Set automatically on creation (Instant.now())
- **createUser**: Set from security context (or "system" if not available)
- **lastUpdateDate**: Updated automatically on every update
- **lastUpdateUser**: Updated from security context on every update

### Logical Delete
- DELETE operation sets `canceled=true` and updates audit fields
- Canceled entities are still retrievable by ID
- List operations may filter out canceled entities (to be decided)

