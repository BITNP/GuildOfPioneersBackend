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

- **Description**: Registers a new user. A valid, non-expired registration ticket code is required. The password is stored as a BCrypt hash. Until the user uploads a custom avatar via `PUT /api/auth/avatar`, the `avatar` field is the default avatar URL (`/uploads/avatars/default`), a reserved object of the `avatars` namespace refreshed from the configured source image at every startup.
- **Authentication**: None.
- **Request Body**: `RegisterRequest`:
  - `phone` (string, required) - phone number; must be a Chinese mobile number (`1[3-9]` followed by 9 digits).
  - `password` (string, required, min 8 chars) - plaintext password, hashed before storing.
  - `userName` (string, required) - display name.
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
      "avatar": "/uploads/avatars/default?v=1720000000000",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - validation failed (e.g. invalid phone format, invalid email, short password), unknown ticket code, or expired ticket code.
  - `409 CONFLICT` - phone is already registered.

### `POST /api/auth/login`

- **Description**: Authenticates a user by username and password. The username lookup is case-insensitive. On success, establishes a server-side session and returns a session cookie. When `rememberMe` is `true`, the session and its cookie are extended to 30 days so the user stays logged in across browser restarts; otherwise the session cookie is browser-session-scoped.
- **Authentication**: None.
- **Request Body**: `LoginRequest`:
  - `username` (string, required) - login identifier.
  - `password` (string, required) - plaintext password.
  - `rememberMe` (boolean, optional, default `false`) - persist the session for 30 days instead of the current browser session.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "/uploads/avatars/1?v=1720000000000",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - missing username or password.
  - `401 UNAUTHORIZED` - invalid username or password.

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
      "avatar": "/uploads/avatars/1?v=1720000000000",
      "phone": "13800000000",
      "email": "alice@example.com",
      "department": "Technology"
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.

### `PUT /api/auth/profile`

- **Description**: Updates the authenticated user's phone and email. The username is not editable.
- **Authentication**: Authenticated session.
- **Request Body**: `UpdateProfileRequest`:
  - `phone` (string, required) - phone number; must be a Chinese mobile number (`1[3-9]` followed by 9 digits).
  - `email` (string, optional) - must be a valid email when provided; may be `null` or empty to clear it.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "/uploads/avatars/1?v=1720000000000",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - validation failed (e.g. invalid phone format or invalid email).
  - `401 UNAUTHORIZED` - not authenticated.
  - `409 CONFLICT` - phone is already registered to another user.

### `PUT /api/auth/avatar`

- **Description**: Uploads (or replaces) the current user's avatar. The image is stored through the Veil storage layer, keyed by the user's id in the `avatars` namespace, and the `avatar` field of the response is the public path `/uploads/avatars/{userId}` with a `?v=` cache-busting version. Re-uploading replaces the stored file in place. Users without a custom avatar keep getting the default avatar URL (`/uploads/avatars/default`).
- **Authentication**: Authenticated session.
- **Request Body**: multipart form-data:
  - `file` (binary, required) - avatar image. Allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Max size 5MB.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "/uploads/avatars/1?v=1720000000000",
      "phone": "13800000000",
      "email": "alice@example.com"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `file` part missing, empty, or unsupported image type.
  - `401 UNAUTHORIZED` - not authenticated.
  - `413 CONTENT_TOO_LARGE` - file exceeds the maximum allowed size.

### `GET /api/todo/actions?taskId={taskId}`

- **Description**: Lists the actions of a task, most recently updated first.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: `taskId` (integer, required) - the owning task's id.
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    [
      {
        "id": 1,
        "taskId": 1,
        "title": "Write report",
        "description": "Draft the final report",
        "createdDate": "2026-08-13T08:00:00.000Z",
        "updatedDate": "2026-08-13T08:00:00.000Z",
        "endDate": null,
        "memberIds": [2, 3]
      }
    ]
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - the task does not exist.

### `GET /api/todo/actions/{actionId}`

- **Description**: Returns a single action with its members.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: `actionId` (integer) - the action's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "taskId": 1,
      "title": "Write report",
      "description": "Draft the final report",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "endDate": null,
      "memberIds": [2, 3]
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - the action does not exist.

### `GET /api/todo/projects`

- **Description**: Lists all projects, most recently updated first. `leaders` and `members` are user summaries (id, userName, avatar) matching the order of `leaderIds` and `memberIds`; deleted users are skipped.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    [
      {
        "id": 1,
        "title": "Autumn Camp",
        "cover": "/uploads/project_covers/1?v=1720000000000",
        "description": "Annual autumn camp",
        "createdDate": "2026-08-13T08:00:00.000Z",
        "updatedDate": "2026-08-13T08:00:00.000Z",
        "endDate": null,
        "leaderIds": [1],
        "memberIds": [2, 3],
        "leaders": [
          {
            "id": 1,
            "userName": "Alice",
            "avatar": "/uploads/avatars/1?v=1720000000000"
          }
        ],
        "members": [
          {
            "id": 2,
            "userName": "Bob",
            "avatar": "/uploads/avatars/default?v=1720000000000"
          },
          {
            "id": 3,
            "userName": "Carol",
            "avatar": "/uploads/avatars/3?v=1720000000000"
          }
        ]
      }
    ]
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.

### `GET /api/todo/projects/{projectId}`

- **Description**: Returns a single project with its leaders, members, and cover image. `cover` is `null` when the project has no stored cover.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: `projectId` (integer) - the project's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "title": "Autumn Camp",
      "cover": "/uploads/project_covers/1?v=1720000000000",
      "description": "Annual autumn camp",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2, 3],
      "leaders": [
        {
          "id": 1,
          "userName": "Alice",
          "avatar": "/uploads/avatars/1?v=1720000000000"
        }
      ],
      "members": [
        {
          "id": 2,
          "userName": "Bob",
          "avatar": "/uploads/avatars/default?v=1720000000000"
        },
        {
          "id": 3,
          "userName": "Carol",
          "avatar": "/uploads/avatars/3?v=1720000000000"
        }
      ]
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - the project does not exist.

### `GET /api/todo/tasks?projectId={projectId}`

- **Description**: Lists the tasks of a project, most recently updated first. `leaders` and `members` are user summaries (id, userName, avatar) matching the order of `leaderIds` and `memberIds`; deleted users are skipped.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: `projectId` (integer, required) - the owning project's id.
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    [
      {
        "id": 1,
        "projectId": 1,
        "title": "Prepare supplies",
        "description": "Buy camping supplies",
        "createdDate": "2026-08-13T08:00:00.000Z",
        "updatedDate": "2026-08-13T08:00:00.000Z",
        "endDate": null,
        "leaderIds": [1],
        "memberIds": [2],
        "leaders": [
          {
            "id": 1,
            "userName": "Alice",
            "avatar": "/uploads/avatars/1?v=1720000000000"
          }
        ],
        "members": [
          {
            "id": 2,
            "userName": "Bob",
            "avatar": "/uploads/avatars/default?v=1720000000000"
          }
        ]
      }
    ]
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - the project does not exist.

### `GET /api/todo/tasks/{taskId}`

- **Description**: Returns a single task with its leaders and members.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: `taskId` (integer) - the task's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "projectId": 1,
      "title": "Prepare supplies",
      "description": "Buy camping supplies",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2],
      "leaders": [
        {
          "id": 1,
          "userName": "Alice",
          "avatar": "/uploads/avatars/1?v=1720000000000"
        }
      ],
      "members": [
        {
          "id": 2,
          "userName": "Bob",
          "avatar": "/uploads/avatars/default?v=1720000000000"
        }
      ]
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - the task does not exist.

### `GET /api/users/{id}`

- **Description**: Returns the profile of a user by id. No privacy filtering is applied yet, so the full profile (including phone and email) is returned.
- **Authentication**: Authenticated session.
- **Request Body**: -
- **Path Parameters**: `id` (integer) - the user's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "/uploads/avatars/1?v=1720000000000",
      "phone": "13800000000",
      "email": "alice@example.com",
      "department": "Technology"
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - no user with the given id exists.

### `GET /uploads/{namespace}/{fileName}`

- **Description**: Serves a stored file from the Veil storage layer via a controller so authorization can be applied later. The final path segment is the object key (an extension is optional and ignored for lookup), e.g. `GET /uploads/avatars/1` serves the avatar with key `1`. Public for the `avatars` namespace. The `avatars` namespace additionally holds the reserved key `default`: the default avatar served to users without their own avatar, refreshed from the configured source image (`app.default-avatar`, active while `app.default-avatar-enabled=true`) at every startup.
- **Authentication**: None.
- **Request Body**: -
- **Path Parameters**:
  - `namespace` (string) - the storage namespace, e.g. `avatars`.
  - `fileName` (string) - the object key; an extension is optional and ignored for lookup, e.g. `1`.
- **Query Parameters**: `v` (string, optional) - cache-busting version (the file's last-modified timestamp).
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: binary file bytes with the matching `Content-Type`.
- **Error Responses**:
  - `404 NOT_FOUND` - no such file or unknown namespace.