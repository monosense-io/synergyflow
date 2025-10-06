# Module Boundaries — SynergyFlow

Date: 2025-10-06
Owner: Architect

---

## Allowed Dependencies Matrix

| Module | May Depend On |
|---|---|
| itsm | security, eventing, sla, sse, audit |
| pm | security, eventing, sla, sse, audit |
| workflow | security, eventing, sla, sse, audit, itsm?, pm? (via SPI only) |
| security | audit |
| eventing | audit |
| sla | eventing, audit |
| sse | eventing, security |
| audit | (none) |

Notes:
- Cross‑module calls must go via `spi/` packages annotated with `@NamedInterface`.
- `internal/` packages are not referenced from other modules; access enforced by tests.

## @NamedInterface Catalog

- `io.monosense.synergyflow.itsm.spi.TicketQueryService`
- `io.monosense.synergyflow.pm.spi.IssueQueryService`
- `io.monosense.synergyflow.workflow.spi.ApprovalQueryService`
- `io.monosense.synergyflow.security.api.BatchAuthService`
- `io.monosense.synergyflow.eventing.api.EventPublisher`
- `io.monosense.synergyflow.sla.api.TimerScheduler`
- `io.monosense.synergyflow.sse.api.SseBroadcaster`
- `io.monosense.synergyflow.audit.api.AuditLogWriter`

## Enforcement Approach

Spring Modulith module declarations:

```java
// io/monosense/synergyflow/itsm/package-info.java
@org.springframework.modulith.ApplicationModule(displayName = "ITSM Module")
package io.monosense.synergyflow.itsm;
```

```java
// io/monosense/synergyflow/itsm/spi/package-info.java
@org.springframework.modulith.NamedInterface
package io.monosense.synergyflow.itsm.spi;
```

Modularity test:

```java
// backend/src/test/java/io/monosense/synergyflow/ModularityTests.java
@Test
void verifiesModularity() {
  ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);
  modules.verify();
}
```

ArchUnit rule (internal access):

```java
// Enforce no access to internal/
noClasses().that().resideOutsideOfPackage("..internal..")
  .should().accessClassesThat().resideInAnyPackage("..internal..")
  .because("Internal packages are implementation details");
```

