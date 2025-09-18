---
id: 11-testing-strategy-spring-modulith
title: 11. Testing Strategy (Spring Modulith)
version: 8.0
last_updated: 2025-09-03
owner: QA
status: Draft
---

## 11. Testing Strategy (Spring Modulith)

### 11.1 Module Isolation Testing
**Objective**: Test individual modules in isolation with proper Spring Modulith patterns

**Testing Patterns**:
```java
@ApplicationModuleTest
@RequiredArgsConstructor
class IncidentManagementTests {
    
    private final IncidentService incidentService;
    
    // Mock external system dependencies (NEVER mock other modules)
    @MockitoBean
    ExternalSystemAdapter externalSystem;
    
    @Test
    void shouldPublishIncidentCreatedEvent(PublishedEvents events) {
        // Test event publication with proper event verification
        var command = new CreateIncidentCommand("Critical Issue", "P1");
        var incident = incidentService.createIncident(command);
        
        // Verify event was published with correct payload
        assertThat(events.ofType(IncidentCreatedEvent.class))
            .matching(IncidentCreatedEvent::getIncidentId, incident.getId())
            .matching(IncidentCreatedEvent::getSeverity, "P1")
            .hasSize(1);
    }
    
    @Test
    void shouldPublishIncidentCreatedEventWithFluentAssertions(AssertablePublishedEvents events) {
        // Alternative fluent assertion style
        incidentService.createIncident(command);
        
        assertThat(events)
            .contains(IncidentCreatedEvent.class)
            .matching(IncidentCreatedEvent::getIncidentId, incident.getId());
    }
    
    @Test  
    void shouldHandleEventReliably(Scenario scenario) {
        // Test cross-module event-driven interactions
        scenario.publish(new IncidentCreatedEvent(incidentId, teamId, "P1", serviceId, Instant.now(), "1.0"))
            .andWaitForEventOfType(NotificationSentEvent.class)
            .matching(event -> event.getIncidentId().equals(incidentId))
            .toArriveAndVerify(event -> {
                // Verify notification was sent with correct details
                assertThat(event.getChannel()).isEqualTo("email");
                assertThat(event.getRecipientTeamId()).isEqualTo(teamId);
            });
    }
    
    @Test
    void shouldHandleEventChainReliably(Scenario scenario) {
        // Test event correlation and problem creation
        scenario.stimulate(() -> incidentService.createIncident(command))
            .andWaitForEventOfType(ProblemCreatedEvent.class)
            .matching(event -> event.getCorrelatedIncidentId().equals(incidentId))
            .toArriveAndVerify(event -> {
                assertThat(event.getStatus()).isEqualTo("INVESTIGATING");
            });
    }
}
```

**Requirements (Spring Modulith Certified)**:
- Module isolation with `@ApplicationModuleTest` (mandatory for each module)
- Event publication verification with `PublishedEvents` and `AssertablePublishedEvents`
- Cross-module interaction testing with `Scenario` API and proper timeout handling
- Mock external dependencies with `@MockitoBean` (NEVER mock other application modules)
- Reliable event delivery validation with transaction rollback testing
- Module boundary compliance with `ApplicationModules.verify()` in CI/CD
- Documentation generation testing with `Documenter` API
- Module health monitoring via `/actuator/modulith` endpoint

**Acceptance Criteria**:
- Each module has comprehensive isolated tests (>95% coverage)
- Event publishing and consumption verified with fluent assertions
- SLA-based scenario testing with realistic timeout values
- Cross-module integration scenarios using event-only communication
- Event failure and retry scenarios validated with persistence testing
- Module verification integrated into CI/CD pipeline
- Event publication registry performance tested under load (2000+ TPS)

### 11.2 Application Module Verification
**Objective**: Ensure architectural compliance with Spring Modulith principles

**Verification Patterns**:
```java
class ModularityTests {
    
    @Test
    void verifyModularity() {
        ApplicationModules.of(SynergyFlowApplication.class).verify();
    }
    
    @Test
    void generateDocumentation() {
        var modules = ApplicationModules.of(SynergyFlowApplication.class);
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeModuleCanvases()
            .writeIndividualModulesAsPlantUml();
    }
}
```

**Requirements**:
- Automatic detection of module boundary violations
- Module dependency cycle detection
- Documentation generation for module relationships
- Architectural compliance validation


## Review Checklist
- Content complete and consistent with PRD
- Acceptance criteria traceable to tests (where applicable)
- Data model references validated (where applicable)
- Event interactions documented (where applicable)
- Security/privacy considerations addressed
- Owner reviewed and status updated

## Traceability
- Features → Data Model: see 18-data-model-overview.md
- Features → Events: see 03-system-architecture-monolithic-approach.md
- Features → Security: see 13-security-compliance-framework.md
- Features → Tests: see 11-testing-strategy-spring-modulith.md
