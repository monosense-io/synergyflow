---
id: arch-12-data-access-mybatis-plus
title: Data Access with MyBatis‑Plus (Spring Boot 3)
owner: Architect (Data)
status: Draft
last_updated: 2025-09-03
links:
  - data-architecture.md
  - module-architecture.md
  - coding-standards.md
  - ../prd/16-technical-specifications.md
---

## Overview

We use MyBatis‑Plus for relational data access with Spring Boot 3 via the
official Spring Boot 3 starter. This complements Spring Modulith by keeping
mappers and DAOs strictly inside module boundaries. Queries use XML or
annotations as needed; conditional builders and interceptors are available via
MyBatis‑Plus.

## Dependencies (Spring Boot 3)

Use the MyBatis‑Plus BOM for version alignment and the Spring Boot 3 starter (reference: MyBatis‑Plus docs).

```groovy
// Gradle (Groovy DSL) – dependency management
dependencyManagement {
  imports {
    mavenBom "com.baomidou:mybatis-plus-bom:3.5.9+"
  }
}

// Spring Boot 3 starter
implementation("com.baomidou:mybatis-plus-spring-boot3-starter")

// SQL parser & plugins (required for PaginationInnerInterceptor since v3.5.9)
implementation("com.baomidou:mybatis-plus-jsqlparser")
```text

## Configuration (application.yml)

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  # If using XML mappers, point to locations
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
```text

## Interceptors

Recommended core interceptors (enable as needed):

- OptimisticLockerInnerInterceptor – optimistic locking
- BlockAttackInnerInterceptor – protect against full‑table updates/deletes
- PaginationInnerInterceptor – paging support (requires mybatis-plus-jsqlparser v3.5.9+)

```java
// @Configuration inside the owning module package
@Bean
MybatisPlusInterceptor mybatisPlusInterceptor() {
  var interceptor = new MybatisPlusInterceptor();
  interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
  interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
  interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
  return interceptor;
}
Important version note (v3.5.9+)

- PaginationInnerInterceptor has been separated; you must include `com.baomidou:mybatis-plus-jsqlparser` to use it.
- Some parser-based inner interceptors (e.g., BlockAttackInnerInterceptor) also rely on jsqlparser.
- See the official Installation chapter for details.

## Mapper Scanning & Modulith Boundaries
- Define `@MapperScan` in a module‑local `@Configuration` class that resides in
  the same package tree as that module (e.g., `io.monosense.synergyflow.incident`).
- Scope the basePackages to that module only. Do not scan across module roots.
- Keep all mappers (`*Mapper.java`) under the module’s package, e.g. `io.monosense.synergyflow.incident.infrastructure.mapper`.

```java
// Example (module-scoped)
@Configuration
@MapperScan(basePackages = "io.monosense.synergyflow.incident.infrastructure.mapper")
class IncidentMybatisConfig { }
```

## Patterns & Conventions

- Use Lombok for DTOs/entities; MapStruct for mapping between entity ↔ domain ↔ DTO (componentModel="spring").
- Prefer `BaseMapper<T>` for simple CRUD; use XML/annotation mappers for complex SQL.
- Keep business logic in services; mappers remain thin data access.
- Use `map-underscore-to-camel-case: true` to align `snake_case` DB columns with camelCase fields.
- Pagination: use MyBatis‑Plus paging APIs or a pagination interceptor; return
  Page objects from repository methods where appropriate.
  - Since 3.5.9+, add `mybatis-plus-jsqlparser` to use `PaginationInnerInterceptor`.
- ID generation: prefer DB‑native sequences/UUID. Avoid H2‑specific key generators.

## Testing (Modulith + Testcontainers)

- Use `@ApplicationModuleTest` for module isolation; do not mock other modules.
- Start real infrastructure via Testcontainers (PostgreSQL, Redis, MinIO, Kafka) – no H2.
- Provide module‑local SQL fixtures or migrations executed before tests.
- Validate boundary rules with `ApplicationModules.verify()` and ensure mappers are not imported across modules.

## Optional Plugins

- P6Spy (via p6spy starter) for SQL logging in non‑prod environments only.
- Dynamic datasource (com.baomidou:dynamic-datasource-spring-boot3-starter) if
  strictly required; configure per module and document tenancy/routing in ADR
  before adoption.

## Modulith Validation Checklist

- [ ] Each module has its own `@MapperScan` limited to its base package
- [ ] No mapper/repository beans exported across modules
- [ ] `ApplicationModules.verify()` passes in CI
- [ ] Tests use Testcontainers, verifying CRUD and transactions at module level
- [ ] Interceptors configured in module config (not globally), unless shared is intentional and documented

## References

- MyBatis‑Plus Spring Boot 3 starter and BOM usage
- Interceptors: Optimistic Locker, Block Attack
- Mapper locations and YAML configuration
