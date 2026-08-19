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

- **Description**: Creates a registration ticket. The generated code is valid until `expiresAt`. The ticket records the department and role the new user is invited into. Only users in the `ADMIN` or `PRESIDIUM` departments may create tickets, and tickets cannot be issued for the `ADMIN` department.
- **Authentication**: Authenticated session. The current user must be a member of the `ADMIN` or `PRESIDIUM` department, and is recorded as the ticket creator.
- **Request Body**: `CreateRegistrationTicketRequest`:
  - `expiresAt` (string, required) - ISO-8601 instant when the ticket expires; must be in the future.
  - `department` (string, required) - the department the new user is invited into; one of `CLINIC`, `TECH`, `SUPPORT`, `MEDIA`, `PRESIDIUM`.
  - `role` (string, required) - the role the new user is invited into; one of `LEADER`, `VICE`, `ADVISOR`, `MEMBER`.
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
      "createdBy": 1,
      "department": "TECH",
      "role": "MEMBER"
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `expiresAt` missing or not in the future, `department` or `role` missing, or `department` is `ADMIN`.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a member of the `ADMIN` or `PRESIDIUM` department.

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
      "email": "alice@example.com",
      "departments": [],
      "isManager": false
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - validation failed (e.g. invalid phone format, invalid email, short password), unknown ticket code, or expired ticket code.
  - `409 CONFLICT` - phone is already registered.

### `POST /api/auth/login`

- **Description**: Authenticates a user by username and password. The username lookup is case-insensitive. On success, establishes a server-side session and returns a session cookie. When `rememberMe` is `true`, the session and its cookie are extended to 30 days so the user stays logged in across browser restarts; otherwise the session cookie is browser-session-scoped. `isManager` is `true` when the user is a member of the `ADMIN` department or holds a `LEADER`, `VICE`, or `ADVISOR` role in any department.
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
      "email": "alice@example.com",
      "departments": [
        {
          "department": "TECH",
          "role": "MEMBER"
        }
      ],
      "isManager": false
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

- **Description**: Returns the currently authenticated user. `isManager` is `true` when the user is a member of the `ADMIN` department or holds a `LEADER`, `VICE`, or `ADVISOR` role in any department.
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
      "departments": [
        {
          "department": "CLINIC",
          "role": "MEMBER"
        }
      ],
      "isManager": false
    }
    ```
  - `departments` (array of objects) - the user's department memberships; each item has `department`, which is one of `CLINIC`, `TECH`, `SUPPORT`, `MEDIA`, `PRESIDIUM`, `ADMIN`, and `role`, which is one of `LEADER`, `VICE`, `ADVISOR`, `MEMBER`.
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
      "email": "alice@example.com",
      "departments": [
        {
          "department": "TECH",
          "role": "MEMBER"
        }
      ],
      "isManager": false
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
      "email": "alice@example.com",
      "departments": [
        {
          "department": "TECH",
          "role": "MEMBER"
        }
      ],
      "isManager": false
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

### `POST /api/todo/actions`

- **Description**: Creates an action under a task with its members. Any member of the owning task (leader or member) may create an action, and so may any user in the `ADMIN` department. When the creating user belongs to the owning project, they are added as a member of the action even when not listed in `memberIds`. Every action member must belong to the owning project. Action members who are not already a leader or member of the owning task are added to it as task members. The action's `createdDate` and `updatedDate` are set to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time.
- **Authentication**: Authenticated session. The current user must be a member of the owning task, or a member of the `ADMIN` department.
- **Request Body**: `CreateActionRequest`:
  - `taskId` (integer, required) - the id of the owning task; must exist.
  - `title` (string, required) - the action title; must not be blank.
  - `description` (string, optional) - the action description; may be `null` or empty.
  - `memberIds` (array of integers, optional) - the user ids to assign as members.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `201 CREATED`
  - **Body**:
    ```json
    {
      "id": 3,
      "taskId": 1,
      "title": "Draft outline",
      "description": "Outline the report",
      "createdDate": "2026-08-15T08:00:00.000Z",
      "updatedDate": "2026-08-15T08:00:00.000Z",
      "endDate": null,
      "memberIds": [1, 2]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `title` missing or blank, a referenced user does not exist, or an assignee does not belong to the owning project.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a member of the owning task.
  - `404 NOT_FOUND` - the owning task does not exist.

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

### `PUT /api/todo/actions/{actionId}`

- **Description**: Updates an action's title and description, optionally replacing its members. The action's members may edit it, and so may any user in the `ADMIN` department (who may edit any action). When `memberIds` is provided, the membership list is replaced entirely; when absent it is left unchanged. Every action member must belong to the owning project. Action members who are not already a leader or member of the owning task are added to it as task members. The action's `updatedDate` is bumped to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time as well.
- **Authentication**: Authenticated session. The current user must be a member of the action, or a member of the `ADMIN` department.
- **Request Body**: `UpdateActionRequest`:
  - `title` (string, required) - the action title; must not be blank.
  - `description` (string, optional) - the action description; may be `null` or empty to clear it.
  - `memberIds` (array of integers, optional) - the user ids to assign as members; replaces the current member list when provided.
- **Path Parameters**: `actionId` (integer) - the action's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 1,
      "taskId": 1,
      "title": "Write the final report",
      "description": "Draft and polish the report",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "updatedDate": "2026-08-15T10:00:00.000Z",
      "endDate": null,
      "memberIds": [2]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `title` missing or blank, a referenced user does not exist, or an assignee does not belong to the owning project.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a member of the action.
  - `404 NOT_FOUND` - the action does not exist.

### `DELETE /api/todo/actions/{actionId}/finish`

- **Description**: Reopens a finished action by clearing its `endDate`. The action's members may reopen it, and so may any user in the `ADMIN` department. The action's `updatedDate` is bumped to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time as well.
- **Authentication**: Authenticated session. The current user must be a member of the action, or a member of the `ADMIN` department.
- **Request Body**: -
- **Path Parameters**: `actionId` (integer) - the action's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: the updated action with `endDate` set to `null`.
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a member of the action.
  - `404 NOT_FOUND` - the action does not exist.

### `PUT /api/todo/actions/{actionId}/finish`

- **Description**: Marks an action as finished by setting its `endDate` to the current time. The action's members may finish it, and so may any user in the `ADMIN` department. The action's `updatedDate` is bumped to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time as well.
- **Authentication**: Authenticated session. The current user must be a member of the action, or a member of the `ADMIN` department.
- **Request Body**: -
- **Path Parameters**: `actionId` (integer) - the action's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: the updated action with `endDate` set to the finish time.
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a member of the action.
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

### `POST /api/todo/projects`

- **Description**: Creates a project with its leaders and members. Only a manager (a member of the `ADMIN` department, or holding a `LEADER`, `VICE`, or `ADVISOR` role in any department) may create projects. The creating user is always added as a leader of the project, even when not listed in `leaderIds`. A user may not appear in both the leader and member lists (including the auto-added creator), and every referenced user must exist. The project's `createdDate` and `updatedDate` are set to the current time. The response carries the project's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: Authenticated session. The current user must be a manager.
- **Request Body**: `CreateProjectRequest`:
  - `title` (string, required) - the project title; must not be blank.
  - `description` (string, optional) - the project description; may be `null` or empty.
  - `leaderIds` (array of integers, optional) - the user ids to assign as leaders.
  - `memberIds` (array of integers, optional) - the user ids to assign as members.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `201 CREATED`
  - **Body**: `TodoProjectUpdateResponse` - the created project without resolved user summaries.
    ```json
    {
      "id": 4,
      "title": "Hackathon",
      "cover": null,
      "description": "Annual hackathon",
      "createdDate": "2026-08-15T08:00:00.000Z",
      "updatedDate": "2026-08-15T08:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2, 3]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `title` missing or blank, a user appears in both `leaderIds` and `memberIds`, or a referenced user does not exist.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a manager.

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

### `PUT /api/todo/projects/{projectId}`

- **Description**: Updates a project's title and description, optionally replacing its leaders and members. The project's leaders may edit it, and so may any user in the `ADMIN` department (who may edit any project). When `leaderIds` or `memberIds` is provided, the corresponding membership list is replaced entirely; when absent it is left unchanged. A user may not appear in both lists, and every referenced user must exist. The project's `updatedDate` is bumped to the current time. The response carries the project's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: Authenticated session. The current user must be a leader of the project, or a member of the `ADMIN` department.
- **Request Body**: `UpdateProjectRequest`:
  - `title` (string, required) - the project title; must not be blank.
  - `description` (string, optional) - the project description; may be `null` or empty to clear it.
  - `leaderIds` (array of integers, optional) - the user ids to assign as leaders; replaces the current leader list when provided.
  - `memberIds` (array of integers, optional) - the user ids to assign as members; replaces the current member list when provided.
- **Path Parameters**: `projectId` (integer) - the project's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: `TodoProjectUpdateResponse` - the updated project without resolved user summaries.
    ```json
    {
      "id": 1,
      "title": "Autumn Camp 2026",
      "cover": "/uploads/project_covers/1?v=1720000000000",
      "description": "Annual autumn camp",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "updatedDate": "2026-08-13T10:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2, 3]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `title` missing or blank, a user appears in both `leaderIds` and `memberIds`, or a referenced user does not exist.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a leader of the project.
  - `404 NOT_FOUND` - the project does not exist.

### `PUT /api/todo/projects/{projectId}/cover`

- **Description**: Stores (or replaces) the cover image of a project. The image is stored through the Veil storage layer, keyed by the project's id in the `project_covers` namespace, and the `cover` field of the response is the public path `/uploads/project_covers/{projectId}` with a `?v=` cache-busting version. The change bumps the project's `updatedDate`. A leader of the project may set its cover, as may any user in the `ADMIN` department.
- **Authentication**: Authenticated session. The current user must be a leader of the project, or a member of the `ADMIN` department.
- **Request Body**: multipart form-data:
  - `file` (binary, required) - cover image. Allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Max size 5MB.
- **Path Parameters**: `projectId` (integer) - the project's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: `TodoProjectUpdateResponse` - the updated project without resolved user summaries.
    ```json
    {
      "id": 1,
      "title": "Autumn Camp",
      "cover": "/uploads/project_covers/1?v=1720000000000",
      "description": "Annual autumn camp",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "updatedDate": "2026-08-15T09:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2, 3]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `file` part missing, empty, or unsupported image type.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a leader of the project.
  - `404 NOT_FOUND` - the project does not exist.
  - `413 CONTENT_TOO_LARGE` - file exceeds the maximum allowed size.

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

### `POST /api/todo/tasks`

- **Description**: Creates a task under a project with its leaders and members. Any member of the owning project (leader or member) may create a task, and so may any user in the `ADMIN` department. The creating user is always added as a leader of the task, even when not listed in `leaderIds`. A user may not appear in both the leader and member lists (including the auto-added creator), and every referenced user must exist. For non-admin creators, every task leader and member must belong to the owning project; admins may assign any existing user. The task's `createdDate` and `updatedDate` are set to the current time, and the owning project's `updatedDate` is bumped to the current time. The response carries the task's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: Authenticated session. The current user must be a member of the owning project, or a member of the `ADMIN` department.
- **Request Body**: `CreateTaskRequest`:
  - `projectId` (integer, required) - the id of the owning project; must exist.
  - `title` (string, required) - the task title; must not be blank.
  - `description` (string, optional) - the task description; may be `null` or empty.
  - `leaderIds` (array of integers, optional) - the user ids to assign as leaders.
  - `memberIds` (array of integers, optional) - the user ids to assign as members.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `201 CREATED`
  - **Body**: `TodoTaskUpdateResponse` - the created task without resolved user summaries.
    ```json
    {
      "id": 5,
      "projectId": 1,
      "title": "Prepare supplies",
      "description": "Buy camping supplies",
      "createdDate": "2026-08-15T08:00:00.000Z",
      "updatedDate": "2026-08-15T08:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `title` missing or blank, a user appears in both `leaderIds` and `memberIds`, a referenced user does not exist, or an assignee is not a member of the owning project (admins are exempt).
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a member of the owning project.
  - `404 NOT_FOUND` - the owning project does not exist.

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

### `PUT /api/todo/tasks/{taskId}`

- **Description**: Updates a task's title and description, optionally replacing its leaders and members. The task's leaders may edit it, and so may any user in the `ADMIN` department (who may edit any task). When `leaderIds` or `memberIds` is provided, the corresponding membership list is replaced entirely; when absent it is left unchanged. A user may not appear in both lists, and every referenced user must exist. For non-admin editors, every task leader and member must belong to the owning project; admins may assign any existing user. The task's `updatedDate` is bumped to the current time, and the owning project's `updatedDate` is bumped to the current time as well. The response carries the task's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: Authenticated session. The current user must be a leader of the task, or a member of the `ADMIN` department.
- **Request Body**: `UpdateTaskRequest`:
  - `title` (string, required) - the task title; must not be blank.
  - `description` (string, optional) - the task description; may be `null` or empty to clear it.
  - `leaderIds` (array of integers, optional) - the user ids to assign as leaders; replaces the current leader list when provided.
  - `memberIds` (array of integers, optional) - the user ids to assign as members; replaces the current member list when provided.
- **Path Parameters**: `taskId` (integer) - the task's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: `TodoTaskUpdateResponse` - the updated task without resolved user summaries.
    ```json
    {
      "id": 1,
      "projectId": 1,
      "title": "Prepare supplies for camp",
      "description": "Buy camping supplies",
      "createdDate": "2026-08-13T08:00:00.000Z",
      "updatedDate": "2026-08-15T10:00:00.000Z",
      "endDate": null,
      "leaderIds": [1],
      "memberIds": [2]
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - `title` missing or blank, a user appears in both `leaderIds` and `memberIds`, a referenced user does not exist, or an assignee is not a member of the owning project (admins are exempt).
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is not a leader of the task.
  - `404 NOT_FOUND` - the task does not exist.

### `GET /api/users`

- **Description**: Returns a brief summary of every user (id, userName, avatar), ordered by id. Private contact fields are not included. Intended for user pickers, such as assigning leaders and members to a new project.
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
        "userName": "Alice",
        "avatar": "/uploads/avatars/1?v=1720000000000"
      },
      {
        "id": 2,
        "userName": "Bob",
        "avatar": "/uploads/avatars/default?v=1720000000000"
      }
    ]
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.

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
      "departments": [
        {
          "department": "CLINIC",
          "role": "MEMBER"
        }
      ],
      "isManager": false
    }
    ```
  - `departments` (array of objects) - the user's department memberships; each item has `department`, which is one of `CLINIC`, `TECH`, `SUPPORT`, `MEDIA`, `PRESIDIUM`, `ADMIN`, and `role`, which is one of `LEADER`, `VICE`, `ADVISOR`, `MEMBER`.
- **Error Responses**:
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - no user with the given id exists.

### `PUT /api/users/{id}`

- **Description**: Updates the phone and email of the user with the given id. The target user may edit their own profile, and any user in the `ADMIN` department may edit any profile. The username is not editable. A phone already owned by the target user is allowed unchanged; only a genuinely new value is checked for uniqueness.
- **Authentication**: Authenticated session. The current user must be the target user, or a member of the `ADMIN` department.
- **Request Body**: `UpdateProfileRequest`:
  - `phone` (string, required) - phone number; must be a Chinese mobile number (`1[3-9]` followed by 9 digits).
  - `email` (string, optional) - must be a valid email when provided; may be `null` or empty to clear it.
- **Path Parameters**: `id` (integer) - the target user's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "id": 2,
      "userName": "Bob",
      "avatar": "/uploads/avatars/2?v=1720000000000",
      "phone": "13900000000",
      "email": "bob@example.com",
      "departments": [
        {
          "department": "TECH",
          "role": "MEMBER"
        }
      ],
      "isManager": false
    }
    ```
- **Error Responses**:
  - `400 BAD_REQUEST` - validation failed (e.g. invalid phone format or invalid email).
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is neither the target user nor a member of the `ADMIN` department.
  - `404 NOT_FOUND` - no user with the given id exists.
  - `409 CONFLICT` - the new phone is already registered to another user.

### `PUT /api/users/{id}/avatar`

- **Description**: Replaces the avatar of the user with the given id. The target user may update their own avatar, and any user in the `ADMIN` department may update any avatar. The image is stored through the Veil storage layer, keyed by the target user's id in the `avatars` namespace, and the `avatar` field of the response is the public path `/uploads/avatars/{userId}` with a `?v=` cache-busting version.
- **Authentication**: Authenticated session. The current user must be the target user, or a member of the `ADMIN` department.
- **Request Body**: multipart form-data:
  - `file` (binary, required) - avatar image. Allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Max size 5MB.
- **Path Parameters**: `id` (integer) - the target user's id.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**: the updated target profile (same shape as `PUT /api/users/{id}`).
- **Error Responses**:
  - `400 BAD_REQUEST` - `file` part missing, empty, or unsupported image type.
  - `401 UNAUTHORIZED` - not authenticated.
  - `403 FORBIDDEN` - the current user is neither the target user nor a member of the `ADMIN` department.
  - `404 NOT_FOUND` - no user with the given id exists.
  - `413 CONTENT_TOO_LARGE` - file exceeds the maximum allowed size.

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