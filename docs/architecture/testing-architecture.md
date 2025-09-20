---
id: arch-08-testing-architecture
title: Testing Architecture
owner: QA
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/11-testing-strategy-spring-modulith.md
  - ../prd/24-spring-modulith-architecture-compliance-summary.md
---

## Module Tests

- @ApplicationModuleTest with PublishedEvents/AssertablePublishedEvents
- Scenario-driven tests for cross-module flows

## Workflow Tests (Flowable)

- Process definition tests using Flowable Spring test support
- Verify timers, user tasks, and event-driven transitions

## Architecture Tests

- ApplicationModules.verify() in CI
- Boundary violation tests; event contract checks

## NFR Tests

- Load, failover, DR drills per Sizing Appendix

## Examples

### Modularity verification (ApplicationModules.verify())

```java
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

// Replace with your Spring Boot application class
import io.monosense.synergyflow.SynergyFlowApplication;

class ModularityTests {

    @Test
    void verifyModularity() {
        ApplicationModules.of(SynergyFlowApplication.class).verify();
    }
}
```

Optionally, generate module documentation as part of a separate test:

```java
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import io.monosense.synergyflow.SynergyFlowApplication;

class ModularityDocsTests {

    @Test
    void generateModuleDocs() {
        var modules = ApplicationModules.of(SynergyFlowApplication.class);
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeModuleCanvases()
            .writeIndividualModulesAsPlantUml();
    }
}
```

### Module isolation test (@ApplicationModuleTest)

```java
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationModuleTest
class ExampleModuleTests {

    @Autowired
    ExampleService exampleService;

    @Test
    void publishesEvent(PublishedEvents events) {
        exampleService.handle(new ExampleCommand("id-123"));

        // Verify event published with expected attributes
        events.ofType(ExampleCreatedEvent.class)
              .matching(ExampleCreatedEvent::id, "id-123")
              .hasSize(1);
    }

    @Test
    void publishesEvent_fluent(AssertablePublishedEvents events) {
        exampleService.handle(new ExampleCommand("id-123"));

        org.assertj.core.api.Assertions.assertThat(events)
            .contains(ExampleCreatedEvent.class)
            .matching(ExampleCreatedEvent::id, "id-123");
    }
}
```

Notes:
- Do not mock other application modules; only mock true external systems.
- Keep tests deterministic; prefer fixed clocks and seeded data.
