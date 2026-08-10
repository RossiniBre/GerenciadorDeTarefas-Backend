# Task Manager

A task management backend built incrementally to learn **Clean Architecture**, **SOLID**, **Design Patterns**, **Domain-Driven Design (DDD)**, and **Spec-Driven Development** using Java.

The project started without frameworks to understand backend architecture and gradually evolved into a Spring Boot application with persistence, authentication, authorization, REST APIs, AI integration, notifications, and automated testing.

---

# What's Implemented

## Phase 1 — Foundation

- Task domain and lifecycle
- Repository abstraction
- In-memory persistence
- Application use cases
- Dependency Injection and Composition Root

---

## Phase 2 — Task Lifecycle and Testing

- Task update, deletion, start and completion
- Domain-enforced state transitions
- Repository improvements
- Unit testing with JUnit 5

---

## Phase 3 — Categories, Priorities and Builder

- Task categories and priorities
- Builder Pattern
- Fluent object construction
- Domain validation improvements

---

## Phase 4 — Users, Ownership and Authentication

- User management and task ownership
- Authentication and authorization
- Secure password hashing
- Token-based authentication
- Login and logout flows
- Domain-specific security errors

---

## Phase 5 — MySQL Persistence and REST API

- MySQL persistence through JDBC
- Database configuration and transaction handling
- REST API implementation
- Request and response DTOs
- Authentication-protected endpoints
- End-to-end API testing with Postman

---

## Phase 6 — AI Task Assistant

- Natural language task management
- Task creation, listing, updating and deletion through the assistant
- Confirmation-based task operations
- Structured assistant responses
- Redis-backed conversation persistence
- User-scoped assistant sessions
- OpenRouter integration

The assistant uses the existing application use cases instead of directly modifying the domain, preserving business rules and authorization.

---

## Phase 7 — Due Dates and Notifications

- Task due dates and reminder rules
- Priority-based notification scheduling
- Notification lifecycle management
- Automatic rescheduling
- Overdue notification workflow
- Integration with the task lifecycle

---

## Phase 8 — Spring Boot Migration

The manually built HTTP infrastructure was migrated to Spring Boot while preserving the existing architecture and business rules.

- Spring Boot application setup
- Dependency Injection through Spring
- Spring MVC controllers
- Configuration through Spring Beans
- Global exception handling
- Authentication context integration

---

## Phase 9 — Backend Refinement

The final backend refinement consolidated authentication, account management, security, and production-oriented infrastructure.

- Password recovery and reset workflow
- Secure password reset tokens
- SMTP email delivery
- Account deletion
- Duplicate email validation
- MySQL persistence for password reset tokens
- Security and authentication refinements
- Final integration testing

The backend is now considered complete for the current portfolio scope.

---

# Testing

The project uses unit, integration, and end-to-end API testing.

### Current Results

- **127 JUnit tests passing**
- **99 Postman tests passing**
- MySQL integration tests
- Real SMTP email delivery tests
- Authentication and authorization flows
- Password recovery and reset flows
- Protected endpoint validation

Run the test suite with:
```bash
mvn test
```

# Architecture

---
```
src/main/java
│
├── domain
│ ├── model
│ ├── repositories
│ ├── assistant
│ ├── exceptions
│ ├── security
│ └── notification
│
├── application
│ └── usecases
│
├── infrastructure
│
│ ├── http
│ │ ├── controllers
│ │ ├── dto
│ │ ├── argumentresolver
│ │ └── exceptionhandler
│ │
│ ├── config
│ │ ├── SecurityConfig
│ │ ├── RepositoryConfig
│ │ ├── TaskUseCaseConfig
│ │ └── NotificationConfig
│ │
│ ├── persistence
│ │
│ └── security
│
└── TaskManagerApplication
```
Dependency direction:

```
Infrastructure
        ↓
Application
        ↓
Domain
```

The domain layer has no dependency on:

* HTTP
* JDBC
* JSON libraries
* Frameworks

---

# Design Patterns Used

| Pattern              | Purpose                           |
| -------------------- | --------------------------------- |
| Repository           | Persistence abstraction           |
| Static Factory       | Controlled entity creation        |
| Builder              | Fluent object construction        |
| Dependency Injection | Use case decoupling               |
| Strategy             | Password hashing abstraction      |
| Adapter              | HTTP and database implementations |
| Exception Hierarchy  | Explicit domain errors            |

---

# Roadmap

| Phase | Status      | Scope |
| ----- |-------------| ----- |
| 1     | Done        | Task creation |
| 2     | Done        | Update, delete, tests |
| 3     | Done        | Categories, priorities, Builder |
| 4     | Done        | Users, authentication, authorization |
| 5     | Done        | MySQL persistence, REST API, HTTP adapters |
| 6     | Done        | AI assistant workflow, conversational memory and task operations |
| 7     | Done        | Due dates and notification workflow |
| 8     | Done        | Migration to Spring Boot |
| 9     | Done        | Backend refinement and production architecture |
| 10    | In progress | Frontend application |
| 11    |             | Deployment and production environment |
----------------------------------------------------------


# Tech Stack

- Java 21
- Spring Boot
- Spring MVC
- Maven
- JUnit 5
- MySQL 8
- JDBC
- Redis
- Docker
- Postman
- OpenRouter API
- Gmail SMTP

---

# Running the Project

Run:

```
TaskManagerApplication.java
```

Requirements:

* Java 21+
* MySQL running
* Docker (recommended)

Environment variables:

```
DB_HOST
DB_PORT
DB_USER
DB_PASSWORD
ASSISTANT_API_KEY
DB_HOST
DB_PORT
DB_USER
DB_PASSWORD
ASSISTANT_API_KEY
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
```

The HTTP server starts locally and exposes the REST endpoints.

Or through IntelliJ:

```
Lifecycle → test
```

---

# Project Goals

The objective is to build a production-style backend while introducing one architectural concept at a time.

Final goals:

* Clean Architecture
* SOLID principles
* Design Patterns
* REST API
* Authentication
* Authorization
* MySQL persistence
* Automated API testing
* AI-powered task assistant
* Conversational task management
* Context-aware assistant sessions
* Safe AI-driven task execution workflow
* Spring Boot migration
* Spring Boot application architecture 
* Dependency injection with Spring
* REST API using Spring MVC