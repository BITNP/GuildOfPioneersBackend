# API Reference

This document records every REST endpoint exposed by the backend and its behavior.
It is the source of truth for the API contract.

## Conventions

### Base URL

All endpoints are served relative to the application context path (default: `/`).

### Authentication and CSRF

The browser uses an HttpOnly site session cookie established by Keycloak OIDC. Provider roles, usernames and emails never grant site membership. Business endpoints require an admitted site user; authenticated but unbound identities receive 403 and must complete admission. See `GET /api/auth/status` for the three authentication states.

All POST, PUT, PATCH and DELETE requests, including anonymous logout and multipart uploads, require the current session's CSRF token. Fetch `GET /api/auth/csrf` and send the returned token using its `headerName` (`X-CSRF-TOKEN`). Fetch again after session changes; a missing or invalid token returns 403 `CSRF_INVALID`, before endpoint authentication or validation. The frontend obtains a fresh token before each write.

Local sessions expire after 30 minutes of inactivity by default; this is not an absolute lifetime or immediate Keycloak revocation. Password login and the local remember-me option have been removed.

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
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a member of the `ADMIN` or `PRESIDIUM` department, and is recorded as the ticket creator.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `PUT /api/auth/avatar`

- **Description**: Uploads (or replaces) the current user's avatar. The image is stored through the object storage layer, keyed by the user's id in the `avatars` namespace, and the `avatar` field of the response is the public path `/uploads/avatars/{userId}` with a `?v=` cache-busting version. Re-uploading replaces the stored file in place. Users without a custom avatar keep getting the default avatar URL (`/uploads/avatars/default`).
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/auth/csrf`

- **Description**: Materializes the masked CSRF token for the current server session. May create an anonymous session. The response is not cached.
- **Authentication**: None.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "headerName": "X-CSRF-TOKEN",
      "token": "opaque-masked-csrf-token"
    }
    ```
- **Error Responses**: -

### `GET /api/auth/login`

- **Description**: Starts backend OIDC login. Lazily discovers Keycloak and redirects to the authorization request entry point. This replaces the removed password-based `POST /api/auth/login`; there is no username/password request or remember-me option.
- **Authentication**: None.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: `returnTo` (string, optional, default `/`) - root-relative site page to return to. External, malformed and authentication-loop paths fall back to `/`.
- **Success Response**:
  - **Status**: `302 Found`
  - **Body**: -
  - `Location`: `/api/auth/oidc/authorize/keycloak`, or `/login?error=provider_unavailable` when discovery fails. No credentials are placed in the redirect URL.
- **Error Responses**: -

### `POST /api/auth/logout`

- **Description**: Invalidates the current site session, including server-side OIDC credentials, and clears the session cookie. Does not invoke Keycloak logout or invalidate other applications' SSO sessions.
- **Authentication**: No login required; a valid CSRF header is required.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `204 No Content`
  - **Body**: -
- **Error Responses**:
  - `403 CSRF_INVALID` - missing or invalid CSRF token.

### `GET /api/auth/me`

- **Description**: Returns the site-owned profile of the admitted user. Identity is resolved by stable site user ID, not a Keycloak username or email. `isManager` is true for ADMIN membership or LEADER/VICE/ADVISOR in any department.
- **Authentication**: OIDC-authenticated session with site membership.
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
          "department": "TECH",
          "role": "MEMBER"
        }
      ],
      "isManager": false
    }
    ```
- **Error Responses**:
  - `401 UNAUTHORIZED` - no OIDC session.
  - `403 FORBIDDEN` - OIDC identity has not completed site admission.
  - `404 NOT_FOUND` - the bound site user no longer exists.

### `GET /api/auth/oidc/authorize/keycloak`

- **Description**: Spring Security OIDC Authorization Code entry point. Creates browser-session-bound state, nonce and PKCE S256 data, then redirects the browser to Keycloak. Normally reached from `GET /api/auth/login`.
- **Authentication**: None.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `302 Found`
  - **Body**: -
  - `Location`: the provider authorization endpoint with protocol parameters. Successful discovery is cached; failed discovery may be retried.
- **Error Responses**:
  - `500 INTERNAL_SERVER_ERROR` - the provider authorization request cannot be constructed. Use the login entry point for a retry page.

### `GET /api/auth/oidc/callback/keycloak`

- **Description**: Handles the provider callback, verifies browser-bound state and the ID token's signature, issuer, audience, expiry and nonce, then rotates the session ID. Uses issuer/subject to resolve an explicitly bound site user; never matches names or emails.
- **Authentication**: Requires the browser session that initiated login and valid provider response.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: `code` and `state` (strings) - authorization code and login state; alternatively the provider may return `error`.
- **Success Response**:
  - **Status**: `302 Found`
  - **Body**: -
  - `Location`: the validated requested page for an admitted user; `/register?redirect=...` for an unbound identity; `/login?error=oidc_failed` on failure or cancellation. No site token is returned in the URL.
- **Error Responses**: -

### `PUT /api/auth/profile`

- **Description**: Updates the authenticated user's phone and email. The username is not editable.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `POST /api/auth/register`

- **Description**: Admits a verified, unbound OIDC identity. Atomically creates the local user, department membership and unique identity binding, rotates the session ID, and establishes business access. No password is accepted or stored. Normally requires a valid ticket; only the configured first administrator identity, while no site administrator exists, can initialize ADMIN/LEADER without a ticket. Default avatar behavior is unchanged.
- **Authentication**: OIDC-authenticated pending-admission session plus valid CSRF header.
- **Request Body**: `RegisterRequest`:
  - `userName` (string, required, max 255) - site display name, trimmed; must be unique ignoring case.
  - `phone` (string, required) - Chinese mobile number matching `1[3-9]` followed by 9 digits; must not belong to another site user.
  - `email` (string, optional, max 255) - valid email, or null/empty.
  - `ticketCode` (string, required for ordinary members, max 255) - valid, non-expired invitation. Optional only for first-administrator initialization.
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `201 Created`
  - **Body**:
    ```json
    {
      "id": 1,
      "userName": "Alice",
      "avatar": "/uploads/avatars/default?v=1720000000000",
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
  - `400 BAD_REQUEST` - invalid profile, missing required ticket, unknown or expired ticket.
  - `401 UNAUTHORIZED` - not OIDC authenticated.
  - `403 FORBIDDEN` - already admitted; this endpoint cannot change memberships.
  - `403 CSRF_INVALID` - missing or invalid CSRF token.
  - `409 CONFLICT` - existing local username/mobile number, existing identity association or a concurrent registration conflict. Contact a maintainer for legacy account binding; duplicate profiles do not imply ownership.

### `GET /api/auth/status`

- **Description**: Reports authentication and site admission separately. Does not expose tokens or passwords. Local profile suggestions are returned only for the current verified unbound identity; email is suggested only when the provider marks it verified. Response is not cached.
- **Authentication**: None.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "state": "onboarding_required",
      "user": null,
      "suggestions": {
        "userName": "Alice",
        "email": "alice@example.com"
      },
      "bootstrapAdmin": false
    }
    ```
  - `state`: `anonymous`, `onboarding_required`, or `active`. `active` includes `user` in the same shape as `/api/auth/me`, and has null suggestions. `anonymous` has null user/suggestions. `bootstrapAdmin` is true only for the configured unbound first administrator while no administrator exists.
- **Error Responses**:
  - `404 NOT_FOUND` - an authenticated site's bound user was deleted.

### `GET /api/tickets/{code}`

- **Description**: Looks up a registration ticket by its code and reports whether it is valid or expired, along with the department and role the holder is invited into. An expired ticket still returns the invited department and role so the client can show what was offered.
- **Authentication**: None.
- **Request Body**: -
- **Path Parameters**: `code` (string) - the registration ticket code.
- **Query Parameters**: -
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    {
      "valid": true,
      "expired": false,
      "department": "TECH",
      "role": "MEMBER",
      "expiresAt": "2026-09-03T04:00:00.000Z"
    }
    ```
- **Error Responses**:
  - `404 NOT_FOUND` - no ticket with the given code exists.

### `GET /api/todo/actions?taskId={taskId}`

- **Description**: Lists the actions of a task, most recently updated first.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `POST /api/todo/actions`

- **Description**: Creates an action under a task with its members. Any member of the owning task (leader or member) may create an action, and so may any user in the `ADMIN` department. When the creating user belongs to the owning project, they are added as a member of the action even when not listed in `memberIds`. Every action member must belong to the owning project. Action members who are not already a leader or member of the owning task are added to it as task members. The action's `createdDate` and `updatedDate` are set to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a member of the owning task, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/todo/actions/{actionId}`

- **Description**: Returns a single action with its members.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `PUT /api/todo/actions/{actionId}`

- **Description**: Updates an action's title and description, optionally replacing its members. The action's members may edit it, and so may any user in the `ADMIN` department (who may edit any action). When `memberIds` is provided, the membership list is replaced entirely; when absent it is left unchanged. Every action member must belong to the owning project. Action members who are not already a leader or member of the owning task are added to it as task members. The action's `updatedDate` is bumped to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time as well.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a member of the action, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `DELETE /api/todo/actions/{actionId}/finish`

- **Description**: Reopens a finished action by clearing its `endDate`. The action's members may reopen it, and so may any user in the `ADMIN` department. The action's `updatedDate` is bumped to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time as well.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a member of the action, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `PUT /api/todo/actions/{actionId}/finish`

- **Description**: Marks an action as finished by setting its `endDate` to the current time. The action's members may finish it, and so may any user in the `ADMIN` department. The action's `updatedDate` is bumped to the current time, and the owning task's `updatedDate` and the owning project's `updatedDate` are bumped to the current time as well.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a member of the action, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/todo/projects`

- **Description**: Lists all projects, most recently updated first. `leaders` and `members` are user summaries (id, userName, avatar) matching the order of `leaderIds` and `memberIds`; deleted users are skipped.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `POST /api/todo/projects`

- **Description**: Creates a project with its leaders and members. Only a manager (a member of the `ADMIN` department, or holding a `LEADER`, `VICE`, or `ADVISOR` role in any department) may create projects. The creating user is always added as a leader of the project, even when not listed in `leaderIds`. A user may not appear in both the leader and member lists (including the auto-added creator), and every referenced user must exist. The project's `createdDate` and `updatedDate` are set to the current time. The response carries the project's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a manager.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/todo/projects/{projectId}`

- **Description**: Returns a single project with its leaders, members, and cover image. `cover` is `null` when the project has no stored cover.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `PUT /api/todo/projects/{projectId}`

- **Description**: Updates a project's title and description, optionally replacing its leaders and members. The project's leaders may edit it, and so may any user in the `ADMIN` department (who may edit any project). When `leaderIds` or `memberIds` is provided, the corresponding membership list is replaced entirely; when absent it is left unchanged. A user may not appear in both lists, and every referenced user must exist. The project's `updatedDate` is bumped to the current time. The response carries the project's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a leader of the project, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `PUT /api/todo/projects/{projectId}/cover`

- **Description**: Stores (or replaces) the cover image of a project. The image is stored through the object storage layer, keyed by the project's id in the `project_covers` namespace, and the `cover` field of the response is the public path `/uploads/project_covers/{projectId}` with a `?v=` cache-busting version. The change bumps the project's `updatedDate`. A leader of the project may set its cover, as may any user in the `ADMIN` department.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a leader of the project, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/todo/tasks?projectId={projectId}`

- **Description**: Lists the tasks of a project, most recently updated first. `leaders` and `members` are user summaries (id, userName, avatar) matching the order of `leaderIds` and `memberIds`; deleted users are skipped.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `POST /api/todo/tasks`

- **Description**: Creates a task under a project with its leaders and members. Any member of the owning project (leader or member) may create a task, and so may any user in the `ADMIN` department. The creating user is always added as a leader of the task, even when not listed in `leaderIds`. A user may not appear in both the leader and member lists (including the auto-added creator), and every referenced user must exist. For non-admin creators, every task leader and member must belong to the owning project; admins may assign any existing user. The task's `createdDate` and `updatedDate` are set to the current time, and the owning project's `updatedDate` is bumped to the current time. The response carries the task's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a member of the owning project, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/todo/tasks/{taskId}`

- **Description**: Returns a single task with its leaders and members.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `PUT /api/todo/tasks/{taskId}`

- **Description**: Updates a task's title and description, optionally replacing its leaders and members. The task's leaders may edit it, and so may any user in the `ADMIN` department (who may edit any task). When `leaderIds` or `memberIds` is provided, the corresponding membership list is replaced entirely; when absent it is left unchanged. A user may not appear in both lists, and every referenced user must exist. For non-admin editors, every task leader and member must belong to the owning project; admins may assign any existing user. The task's `updatedDate` is bumped to the current time, and the owning project's `updatedDate` is bumped to the current time as well. The response carries the task's own fields and the member user ids only; user name/avatar summaries are resolved by the `GET` endpoints.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be a leader of the task, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /api/todo/tree`

- **Description**: Returns the todo hierarchy as a tree, most recently updated first within each level. When `userId` is given, the tree is filtered to that user's related subtree: projects they lead or belong to, tasks they lead or belong to, and actions they carry out; ancestor nodes are kept so related descendants remain reachable. When `userId` is absent, every project is returned. The `depth` parameter controls how many levels are included: `1` returns projects only, `2` adds tasks, and `3` adds actions. When a level is below `depth`, its child list is omitted from the response.
- **Authentication**: OIDC-authenticated session with site membership.
- **Request Body**: -
- **Path Parameters**: -
- **Query Parameters**:
  - `userId` (integer, optional) - the user whose related subtree is returned; when absent, all projects are returned.
  - `depth` (integer, optional, default `3`, allowed `1`-`3`) - the number of hierarchy levels to include.
- **Success Response**:
  - **Status**: `200 OK`
  - **Body**:
    ```json
    [
      {
        "id": 1,
        "title": "Autumn Camp",
        "tasks": [
          {
            "id": 1,
            "projectId": 1,
            "title": "Prepare supplies",
            "actions": [
              {
                "id": 1,
                "taskId": 1,
                "title": "Write report",
                "endDate": null
              }
            ]
          }
        ]
      }
    ]
    ```
  - With `depth=1`, each project carries only `id` and `title` (no `tasks` key); with `depth=2`, each task carries no `actions` key.
- **Error Responses**:
  - `400 BAD_REQUEST` - `depth` is outside the allowed `1`-`3` range.
  - `401 UNAUTHORIZED` - not authenticated.
  - `404 NOT_FOUND` - no user with the given `userId` exists.
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `GET /api/users`

- **Description**: Returns a brief summary of every user (id, userName, avatar), ordered by id. Private contact fields are not included. Intended for user pickers, such as assigning leaders and members to a new project.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `GET /api/users/{id}`

- **Description**: Returns the profile of a user by id. No privacy filtering is applied yet, so the full profile (including phone and email) is returned.
- **Authentication**: OIDC-authenticated session with site membership.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.

### `PUT /api/users/{id}`

- **Description**: Updates the phone and email of the user with the given id. The target user may edit their own profile, and any user in the `ADMIN` department may edit any profile. The username is not editable. A phone already owned by the target user is allowed unchanged; only a genuinely new value is checked for uniqueness.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be the target user, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `PUT /api/users/{id}/avatar`

- **Description**: Replaces the avatar of the user with the given id. The target user may update their own avatar, and any user in the `ADMIN` department may update any avatar. The image is stored through the object storage layer, keyed by the target user's id in the `avatars` namespace, and the `avatar` field of the response is the public path `/uploads/avatars/{userId}` with a `?v=` cache-busting version.
- **Authentication**: OIDC-authenticated session with site membership. The current user must be the target user, or a member of the `ADMIN` department.
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
  - `403 FORBIDDEN` - the verified OIDC identity has not completed site admission.
  - `403 CSRF_INVALID` - missing or invalid CSRF header.

### `GET /uploads/{namespace}/{fileName}`

- **Description**: Serves a stored file from the object storage layer via a controller so authorization can be applied later. The final path segment is the object key (an extension is optional and ignored for lookup), e.g. `GET /uploads/avatars/1` serves the avatar with key `1`. Public for the `avatars` namespace. The `avatars` namespace additionally holds the reserved key `default`: the default avatar served to users without their own avatar, refreshed from the configured source image (`app.default-avatar`, active while `app.default-avatar-enabled=true`) at every startup.
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
