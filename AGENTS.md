# AGENTS.md

## Project Overview

This is a production-oriented Spring Boot backend project.

The goal is to build a maintainable, scalable backend following standard Spring practices.
Prefer clear and explicit code over clever abstractions. Avoid unnecessary complexity.

---

# General Principles

## Write Production-Quality Code

Always prioritize:

1. Correctness
2. Maintainability
3. Readability
4. Performance
5. Simplicity

Do not introduce unnecessary frameworks, dependencies, or abstractions.

Before adding a new dependency, consider whether the functionality can be implemented cleanly using existing Spring features.

---

# Architecture

Use a layered architecture:

```

Controller
|
Service
|
Repository
|
Database

```

Recommended package structure:

```

com.example.project

├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── exception
├── config
└── util

````

---

# Controller Layer

Responsibilities:

- Handle HTTP requests
- Validate input
- Return HTTP responses
- Convert DTOs

Controllers should NOT:

- Contain business logic
- Directly access repositories
- Perform complex data processing

Example:

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.create(request);
    }
}
````

---

# Service Layer

The service layer contains business logic.

Responsibilities:

* Business rules
* Transaction boundaries
* Coordination between repositories

Example:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        // business logic here
    }
}
```

---

# Repository Layer

Repositories handle database access only.

Do not put business logic here.

Preferred approaches:

* Spring Data JPA for simple CRUD
* JPQL/native SQL/JdbcTemplate/jOOQ for complex queries

Avoid creating unnecessary repository methods.

Do not use ORM relationships (`@OneToMany`, `@ManyToMany`, etc.) unless there is a clear reason. Prefer explicit queries to avoid hidden database operations.

---

# Database Rules

## Entity Design

Entities represent database tables.

Rules:

* Do not expose entities directly in REST APIs
* Use DTOs for API input/output
* Avoid excessive bidirectional relationships
* Avoid lazy-loading problems
* Keep entity logic simple

Preferred flow:

```
Entity
  |
Mapper
  |
DTO
```

---

## Database Migration

All schema changes must use migrations.

Use:

```
src/main/resources/db/migration
```

Example:

```
V1__create_users.sql
V2__add_user_email.sql
```

Never manually modify production databases.

---

# DTO Rules

Use separate DTO classes.

Structure:

```
dto/

├── request
│   ├── CreateUserRequest
│   └── UpdateUserRequest
│
└── response
    └── UserResponse
```

Do not expose internal database structures through APIs.

---

# Exception Handling

Use centralized exception handling.

Preferred:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

}
```

Do not write repeated exception handling in every controller.

Create meaningful exceptions:

```
UserNotFoundException
PermissionDeniedException
InvalidRequestException
```

Avoid:

```java
catch(Exception e)
```

unless there is a very specific reason.

---

# Validation

Validate incoming requests.

Use:

```java
@Valid
@NotNull
@NotBlank
@Size
@Email
```

Do not manually validate simple fields inside controllers.

---

# Transaction Rules

Use transactions explicitly.

Write operations:

```java
@Transactional
```

Read-only operations:

```java
@Transactional(readOnly = true)
```

Avoid long-running transactions.

---

# Lombok Rules

Allowed:

```java
@Getter
@Setter
@Builder
@RequiredArgsConstructor
```

Avoid:

```java
@Data
```

on JPA entities.

Reason:

Generated `equals()`, `hashCode()`, and `toString()` may cause problems with Hibernate entities.

---

# Logging

Use SLF4J.

Preferred:

```java
@Slf4j
```

or:

```java
private static final Logger log =
        LoggerFactory.getLogger(MyClass.class);
```

Do not use:

```java
System.out.println()
```

Logs should contain:

* Important state changes
* Errors
* Warnings

Never log:

* Passwords
* Tokens
* Sensitive information

---

# Configuration

Use:

```
application.yml
```

Prefer environment variables:

```yaml
spring:
  datasource:
    url: ${DB_URL}
```

Never hardcode:

* Database passwords
* API keys
* Secrets

---

# Testing

Important business logic should have tests.

Preferred:

* Unit tests for services
* Integration tests for database/API behavior

Use:

```
src/test/java
```

Prefer integration tests:

```java
@SpringBootTest
```

for full application behavior.

---

# API Design

Follow REST conventions.

Examples:

```
GET    /users
GET    /users/{id}

POST   /users

PUT    /users/{id}

DELETE /users/{id}
```

Use correct HTTP status codes:

```
200 OK
201 CREATED
204 NO_CONTENT
400 BAD_REQUEST
401 UNAUTHORIZED
403 FORBIDDEN
404 NOT_FOUND
500 INTERNAL_SERVER_ERROR
```

---

# Security

When implementing authentication:

* Use Spring Security
* Store passwords using BCrypt
* Never store plaintext passwords
* Validate authorization on the backend

Never trust client-side permission checks.

---

# Code Style

## Naming

Classes:

```
UserService
OrderController
PaymentRepository
```

Methods:

```
createUser()
findById()
calculatePrice()
```

Variables:

```
userRepository
orderList
```

Avoid vague names:

```
doSomething()
manager()
helper()
util()
```

unless their purpose is obvious.

---

# Avoid

Do NOT:

* Put business logic in controllers
* Put SQL in controllers
* Return entities directly from APIs
* Catch generic exceptions everywhere
* Add unnecessary dependencies
* Create unnecessary interfaces
* Use reflection magic when simple code works
* Optimize before measuring
* Hide complicated database operations behind ORM magic

---

# Before Implementing New Features

Always consider:

1. What is the API contract?
2. What database changes are needed?
3. What validation is required?
4. What exceptions can happen?
5. What tests should exist?

---

# Preferred Development Workflow

For every feature:

1. Design API DTOs
2. Design database changes
3. Implement repository layer
4. Implement service logic
5. Implement controller
6. Add validation
7. Add exception handling
8. Add tests

Keep commits small and focused.
