# Order-Service AGENTS.md

## Project Overview
Multi-module Gradle project (Spring Boot 4.1.0, Java 25):
- `order-service` (root) - Spring Boot web app with REST API
- `outbox-event-publisher` - shared library for transactional outbox pattern, published to Maven Local & GitHub Packages

## Key Commands
| Task | Command |
|------|---------|
| Build all | `./gradlew build` |
| Run tests (all modules) | `./gradlew test` |
| Run single module tests | `./gradlew :order-service:test` or `./gradlew :outbox-event-publisher:test` |
| Run application | `./gradlew bootRun` |
| Publish outbox-event-publisher locally | `./gradlew :outbox-event-publisher:publishToMavenLocal` |
| Publish to GitHub Packages | `./gradlew :outbox-event-publisher:publish` (requires `gpr.user`/`gpr.key` or `USERNAME`/`TOKEN` env vars) |

## Architecture Notes
- **Entry point**: `OrderServiceApplication.java` (Spring Boot)
- **Database**: PostgreSQL with schema.sql (runs on startup, creates ENUM types + tables)
- **Message broker**: RabbitMQ (exchange: `es-orders-exchange`, queue: `es-orders-queue`, routing: `order.#`)
- **Outbox pattern**: `outbox-event-publisher` module handles reliable event publishing via `OutboxProcessor` + `OutboxEventManager`
- **Testcontainers**: Auto-configured via `TestcontainersConfiguration` (PostgreSQL + RabbitMQ) - requires Docker

## Required Environment
- Java 25 (Gradle toolchain auto-downloads)
- Docker (for Testcontainers integration tests)
- PostgreSQL + RabbitMQ locally for manual dev runs (see `application.yaml` for defaults)

## Test Configuration
- Integration tests use `@ServiceConnection` with Testcontainers (no manual container setup needed)
- Test profile: `application-test.yaml` (uses `localhost` for DB/RabbitMQ - Testcontainers maps ports)
- Run single test: `./gradlew :order-service:test --tests "com.electronics.store.order_service.controllers.OrderControllerTest"`

## Module Dependencies
- `order-service` depends on `outbox-event-publisher` via `implementation project(':outbox-event-publisher')`
- `outbox-event-publisher` is a standalone library (no Spring Boot app, only `spring-boot-starter-data-jpa`)

## Gotchas
- Schema uses PostgreSQL ENUM types - `ddl-auto: validate` means schema must match exactly
- Outbox processor runs on single-thread executor (`outbox-processor` thread)
- GitHub Packages publishing requires credentials (set via gradle properties or env vars)
- Lombok annotation processing enabled for both main and test sources