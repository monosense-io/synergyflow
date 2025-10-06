# ADR 0001: Standardize Backend Build on Gradle (Kotlin DSL)

Status: Accepted
Date: 2025-10-06
Deciders: Architecture Group
Impacted areas: Backend build, CI/CD, docs

## Context

Early drafts referenced both Maven (including Failsafe for integration tests) and Gradle. Mixed guidance risks confusion in build scripts, CI pipelines, and developer onboarding. The broader architecture package already centers on Gradle 8.10+ with Kotlin DSL and includes a comprehensive Gradle guide.

## Decision

Use Gradle 8.10+ with Kotlin DSL as the single backend build system.

- Files: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Tests: JUnit 5 with a dedicated Gradle `integrationTest` source set (no Maven Failsafe)
- Tasks wired in CI: `clean test integrationTest build`
- Spring Boot tasks: `bootRun`, `bootBuildImage`

## Consequences

- Remove/replace Maven references (`pom.xml`, `mvn ...`, Failsafe) across docs.
- CI examples and developer guides switch to Gradle commands.
- Optional Flyway migration via app startup or Gradle plugin if later adopted.

## Alternatives Considered

- Maven + Failsafe: familiar but duplicates config versus existing Gradle documentation; less aligned with Kotlin ecosystem and our guide.

## References

- docs/architecture/gradle-build-config.md (source of truth)
- docs/architecture/solution-architecture.md

