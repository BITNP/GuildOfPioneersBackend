# API Reference

This document records every REST endpoint exposed by the backend and its behavior.
It is the source of truth for the API contract.

## Conventions

### Base URL

All endpoints are served relative to the application context path (default: `/`).

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| `200 OK` | Successful read / update |
| `201 CREATED` | Resource created |
| `204 NO_CONTENT` | Successful delete |
| `400 BAD_REQUEST` | Invalid request body or parameters |
| `401 UNAUTHORIZED` | Missing or invalid authentication |
| `403 FORBIDDEN` | Authenticated but not authorized |
| `404 NOT_FOUND` | Resource does not exist |
| `500 INTERNAL_SERVER_ERROR` | Unexpected server error |

### Error Response Shape

Errors returned by the centralized exception handler follow this shape:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "User with id 1 not found",
  "path": "/users/1",
  "timestamp": "2026-08-03T12:00:00.000Z"
}
```

---

## Endpoints

<!--
Each endpoint must be documented using the format below.
Keep entries sorted by path, then by HTTP method.

### `METHOD /path`

- **Description**: What the endpoint does.
- **Authentication**: Required roles, or `None`.
- **Request Body**: DTO name and fields (name, type, required, description). `-` if none.
- **Path Parameters**: name, type, description. `-` if none.
- **Query Parameters**: name, type, required, description. `-` if none.
- **Success Response**:
  - **Status**: `200 OK` (or appropriate code)
  - **Body**:
    ```json
    {}
    ```
- **Error Responses**:
  - `404 NOT_FOUND` - when ...
-->

### `POST /api/admin/tickets`

- **Description**: Creates a registration ticket. The generated code is valid until `expiresAt`. Admin/staff authorization is not yet enforced; any authenticated user can create tickets for now.
- **Authentication**: Authenticated session. The current user is recorded as the ticket creator.
- **Request Body**: `CreateRegistrationTicketRequest`:
  - `expiresAt` (string, required) - ISO-8601 instant when the ticket expires; must be in the future.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `201 CREATED`
  - **Body**:
    ```json
    {
      "id": 1,
      "code": "K7M2P9Q4R6TW",
      "createdAt": "2026-08-03T04:00:00.000Z",
      "expiresAt": "2026-09-03T04:00:00.000Z",
      "createdBy": 1
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `expiresAt` missing or not in the future.
  - `401 UNAUTHORIZED` - not authenticated.

### `POST /api/auth/register`

- **Description**: Registers a new user. A valid, non-expired registration ticket code is required. The password is stored as a BCrypt hash.
- **Authentication**: None.
- **Request Body**: `RegisterRequest`:
  - `phone` (string, required) - login identifier.
  - `password` (string, required, min 8 chars) - plaintext password, hashed before storing.
  - `userName` (string, required) - display name.
  - `avatar` (string, required) - avatar URL.
  - `ticketCode` (string, required) - registration ticket code; must exist and not be expired.
  - `email` (string, optional) - must be a valid email when provided.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `201 CREATED`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "https://example.com/avatar.png",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - validation failed, invalid email, unknown ticket code, or expired ticket code.
  - `409 CONFLICT` - phone is already registered.

### `POST /api/auth/login`

- **Description**: Authenticates a user by phone and password. On success, establishes a server-side session and returns a session cookie.
- **Authentication**: None.
- **Request Body**: `LoginRequest`:
  - `phone` (string, required) - login identifier.
  - `password` (string, required) - plaintext password.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "https://example.com/avatar.png",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - missing phone or password.
  - `401 UNAUTHORIZED` - invalid phone or password.

### `POST /api/auth/logout`

- **Description**: Invalidates the current session and clears the session cookie.
- **Authentication**: None (works whether or not a session exists).
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `204 NO_CONTENT`
  - **Body**: -
- **Error Responses**: -

### `GET /api/auth/me`

- **Description**: Returns the currently authenticated user.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "https://example.com/avatar.png",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.