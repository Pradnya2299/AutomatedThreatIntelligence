# ADR-002: Java 21 and Spring Boot 3

## Status

Accepted (Phase 1)

## Context

The stack was specified: Java 21, Spring Boot 3.x, Maven, Spring Web/Kafka/Data JPA/Security/Validation, Spring AI where appropriate, Flyway.

## Decision

- Java 21 LTS, Maven multi-module parent under `backend/`.
- Spring Boot **3.4.x** (3.x line) with Boot parent BOM.
- One module per service; **no shared Java library in Phase 1**.
- Spring AI is a dependency **placeholder** on ai-service only; no OpenAI calls in Phase 1.
- Maven Wrapper committed so CI and laptops do not depend on a global Maven install.

## Consequences

- Team uses a single idiomatic stack (JPA, actuator, Kafka binders).
- Adding a `backend/contracts` module later is backward compatible.
- Spring AI version must be aligned with Boot in Phase 4.

## Alternatives considered

- Quarkus/Micronaut: extra training cost, spec says Spring Boot.
- Gradle: spec says Maven.
- TypeScript backend: forbidden by spec.
