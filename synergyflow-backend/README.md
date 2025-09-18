# SynergyFlow Backend (Spring Modulith)

- Java 21, Gradle 8 (Kotlin DSL)
- Spring Boot 3.x (pinned to latest stable)
- Spring Modulith for module boundaries and testing
- Testcontainers, JUnit 5, AssertJ
- JaCoCo coverage gates

## Versions (at scaffold time)

- Spring Boot: 3.5.6
- Spring Modulith BOM: 1.4.3
- Testcontainers BOM: 1.21.3
- Lombok: 1.18.42
- MapStruct: 1.6.3

Update policy: track latest stable Boot 3.x; apply monthly patch bumps and minor upgrades after smoke tests.

## Tasks

- Build: `./gradlew build`
- Unit tests: `./gradlew test`
- Architecture tests: `./gradlew architectureTest`
- Integration tests: `./gradlew integrationTest`
- Coverage report: `./gradlew jacocoTestReport` (HTML in `build/reports/jacoco/test/html/index.html`)
- Run app: `./gradlew bootRun`

## Module Boundaries

All modules have been created with `package-info.java` containing `@ApplicationModule` annotations:
- `user` (with `user::api` named interface)
- `team` (depends only on `user::api` per ADR-0002)
- `incident`
- `problem`
- `change`
- `knowledge`
- `cmdb`
- `notification`
- `workflow`
- `integration`

Verify with:

```java
ApplicationModules.of(SynergyFlowApplication.class).verify();
```

## Test Support

Comprehensive test support utilities under `src/test/java/io/monosense/synergyflow/testsupport/**`:

- `containers/`: Reusable Testcontainers (PostgreSQL, Redis, Kafka) with automatic startup and reuse
- `events/`: Spring Modulith event testing helpers with fluent assertions
- `api/`: API client helpers for REST testing
- `time/`: Deterministic clock utilities for time-based testing

### Container Reuse Configuration

To enable container reuse for faster test execution, set the following environment variable:

```bash
export TESTCONTAINERS_REUSE_ENABLE=true
```

This allows containers to be reused across test runs, significantly reducing startup time.

## Next Steps

- Implement business logic in each module according to their domain responsibilities.
- Add more comprehensive event-driven integration tests as features are developed.

