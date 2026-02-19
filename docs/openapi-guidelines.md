# OpenAPI & REST guidelines

This document defines the conventions for:
- OpenAPI contract updates (`api/openapi.yaml`)
- REST adapter annotations in code (springdoc / swagger-annotations)

It exists to keep feature specs focused on business logic.

## 1) OpenAPI is the contract

- The source of truth for endpoints, payloads, status codes and examples is `api/openapi.yaml`.
- Every feature implementation must keep code and contract aligned.

## 2) Tagging

Use a small, consistent set of tags to group endpoints.

Recommended baseline tags:
- `System` (health/smoke endpoints, e.g. `/api/health`)
- `Folders`
- `Documents`
- `Permissions`
- ...

Rules:
- Every operation MUST have exactly one primary tag.
- The tag MUST be present both:
  - in `api/openapi.yaml`
  - in the REST adapter via `@Tag(name = "...")`

## 3) operationId

- Every operation MUST have a stable `operationId`.
- The `operationId` in `api/openapi.yaml` MUST match the `@Operation(operationId = "...")` in code.

## 4) Responses and examples

- Every `2xx` response MUST define:
  - `description`
  - `content` (when applicable)
  - `schema`
  - at least one `example`

- Every `4xx/5xx` response MUST follow the error model described in `docs/error-model.md`.

## 5) REST adapter annotations (springdoc)

Use `io.swagger.v3.oas.annotations.*`.

### Class-level

- Add `@Tag(name = "...", description = "...")`.
- Keep the controller thin (mapping + validation + delegation).

### Method-level

- Add `@Operation` with:
  - `operationId`
  - `summary`
  - `description`

- Add `@ApiResponses` with `@ApiResponse` entries.
- For successful responses, include `@ExampleObject` values aligned with `api/openapi.yaml`.

## 6) Endpoint conventions

- Prefer nouns for resources (`/api/folders`, `/api/documents`).
- Use plural resources.
- Use correct HTTP status codes:
  - `200` OK for reads/updates
  - `201` Created for creates
  - `204` No Content for deletes (if no body)
  - `400` for validation
  - `403` for authorization
  - `404` for not found

## 7) Consistency checks (manual)

Before considering a feature DONE:
- OpenAPI operation exists in `api/openapi.yaml`
- `operationId` matches code annotations
- examples match actual JSON responses
- error payloads match `docs/error-model.md`
