---
id: 03-system-architecture-monolithic-approach
title: 3. System Architecture (Monolithic Approach)
version: 8.0
last_updated: 2025-09-03
owner: Architect
status: Draft
---

## 3. System Architecture (Monolithic Approach)

### 3.1 Application Layers

```

 
┌─────────────────────────────────────┐
│          Presentation Layer         │
│  ┌─────────────────┐ ┌─────────────┐│
│  │   Web UI        │ │  REST API   ││
│  │ (Next.js + shadcn/ui + Tailwind) │ │ (Spring MVC)││
│  └─────────────────┘ └─────────────┘│
├─────────────────────────────────────┤
│           Business Layer            │
│  ┌─────────────────────────────────┐│
│  │     ITIL Process Services       ││
│  │ (Service, Security, Validation) ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│         Data Access Layer           │
│  ┌─────────────────────────────────┐│
│  │     MyBatis-Plus Mappers        ││
│  └─────────────────────────────────┘│
├─────────────────────────────────────┤
│         Integration Layer           │
│  ┌─────────────────────────────────┐│
│  │   External System Adapters      ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### 3.2 Spring Modulith Application Modules

**ITIL Process Modules (Bounded Contexts)**

```java
// package-info.java
@ApplicationModule
package io.monosense.synergyflow.incident;

import org.springframework.modulith.ApplicationModule;
```
- **Incident Management** (`itsm.incident`)
  - Incident logging, categorization, priority/urgency matrix
  - SLA countdown timers and breach notifications
  - Major incident procedures and service impact assessment
  - Published events: `IncidentCreatedEvent`, `IncidentAssignedEvent`, `IncidentResolvedEvent`, `SlaBreachedEvent`
  - **Dependencies**: Event-driven communication only (no direct module dependencies)

```java
// package-info.java
@ApplicationModule
package io.monosense.synergyflow.problem;

import org.springframework.modulith.ApplicationModule;
```
- **Problem Management** (`itsm.problem`)
  - Root cause analysis workflows and Known Error Database (KEDB)
  - Problem record creation from incident patterns
  - Published events: `ProblemCreatedEvent`, `RootCauseIdentifiedEvent`, `KnownErrorCreatedEvent`
  - **Dependencies**: Zero dependencies (pure event-driven communication)

```java
// package-info.java
@ApplicationModule
package io.monosense.synergyflow.change;

import org.springframework.modulith.ApplicationModule;
```
- **Change Management** (`itsm.change`)
  - Change Advisory Board (CAB) approval workflows
  - Risk assessment and change calendar management
  - Published events: `ChangeRequestedEvent`, `ChangeApprovedEvent`, `ChangeDeployedEvent`, `ChangeRejectedEvent`, `ChangeImpactAssessmentRequestedEvent`
  - Consumes events: `ChangeImpactAssessedEvent` (from CMDB)
  - **Dependencies**: Zero direct module dependencies (event-driven impact assessment; no synchronous CMDB calls)

```java
// package-info.java
@ApplicationModule
package io.monosense.synergyflow.knowledge;

import org.springframework.modulith.ApplicationModule;
```
- **Knowledge Management** (`itsm.knowledge`)
  - Article creation, approval workflows, full-text search
  - AI-powered article suggestions and usage analytics
  - Published events: `ArticleCreatedEvent`, `ArticleUpdatedEvent`, `ArticleSearchedEvent`, `ArticleExpiredEvent`
  - **Dependencies**: Zero dependencies (pure event-driven communication)

```java
// package-info.java
@ApplicationModule
package io.monosense.synergyflow.cmdb;

import org.springframework.modulith.ApplicationModule;
```

- **Configuration Management Database** (`itsm.cmdb`)
  - CI registration, relationship mapping, impact analysis
  - Asset lifecycle management and discovery integration
  - Published events: `CICreatedEvent`, `CIRelationshipChangedEvent`, `AssetUpdatedEvent`, `ChangeImpactAssessedEvent`
  - **API**: Optional read API for external tooling; ITIL modules consume CMDB via events (no direct dependencies)

```java
// package-info.java for API interface
@NamedInterface("api")
package io.monosense.synergyflow.cmdb.api;

import org.springframework.modulith.NamedInterface;
```

**Core Support Modules**

```java
@ApplicationModule
package io.monosense.synergyflow.user;
```
- **User Management** (`itsm.user`)
  - Authentication, user profiles, LDAP integration
  - Published events: `UserCreatedEvent`, `UserActivatedEvent`, `UserDeactivatedEvent`
  - **Dependencies**: None (foundation module)

```java
// package-info.java
@ApplicationModule(allowedDependencies = "user :: api")
package io.monosense.synergyflow.team;

import org.springframework.modulith.ApplicationModule;
```

- **Team & Organization** (`itsm.team`)
  - Team hierarchy, skill matrices, workload balancing
  - User-to-team relationship management and delegation
  - Published events: `TeamCreatedEvent`, `TeamMemberAssignedEvent`, `SkillUpdatedEvent`, `UserAssignedToTeamEvent`
  - **API**: Named interface for team queries and assignment services

```java
// package-info.java for API interface
@NamedInterface("api")
package io.monosense.synergyflow.team.api;

import org.springframework.modulith.NamedInterface;
```
  - **Dependencies**: User API for identity resolution only

**Infrastructure Modules**

```java
@ApplicationModule
package io.monosense.synergyflow.workflow;
```
- **Workflow Engine** (`itsm.workflow`)
  - Business rules engine, state machine management
  - ITIL process orchestration and custom workflow builder
  - Event listeners: All domain events for workflow triggers
  - **Architecture**: Pure event listener - no direct module dependencies

```java
@ApplicationModule  
package io.monosense.synergyflow.notification;
```
- **Notification System** (`itsm.notification`)
  - Multi-channel delivery (email, Telegram, in-app)
  - Notification rules engine and user preferences
  - Event listeners: All domain events for notification triggers
  - **Architecture**: Pure event listener - no direct module dependencies

```java
// package-info.java
@ApplicationModule
package io.monosense.synergyflow.integration;

import org.springframework.modulith.ApplicationModule;
```
- **External Integration** (`itsm.integration`)
  - System adapters (LDAP, monitoring, email)
  - API gateway patterns and webhook handling
  - Circuit breaker and retry logic for external calls
  - Event listeners: `UserCreatedEvent`, `UserActivatedEvent`
  - **Dependencies**: Zero dependencies (authentication context via events)
  - **Architecture**: Pure event-driven integration

### 3.3 Event-Driven Inter-Module Communication

**Domain Event Patterns**:
```java
// Example: Incident Management publishes events with proper aggregate pattern
@Service
@RequiredArgsConstructor
@Transactional
public class IncidentService {
    private final ApplicationEventPublisher events;
    private final IncidentRepository repository;
    
    public void createIncident(CreateIncidentCommand command) {
        // Create incident aggregate
        Incident incident = new Incident(command);
        repository.save(incident);
        
        // Publish domain event for other modules (transactional)
        events.publishEvent(new IncidentCreatedEvent(
            incident.getId(), 
            incident.getAssignedTeamId(), 
            incident.getSeverity(),
            incident.getAffectedServiceId(),
            Instant.now(),
            "1.0" // Event schema version
        ));
    }
}

// Example: Event listeners in notification module (pure event-driven)
@Component
public class NotificationEventHandler {
    
    private final NotificationService notificationService;
    
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(IncidentCreatedEvent event) {
        try {
            // Send notifications in separate transaction
            notificationService.notifyTeamByEvent(event);
        } catch (Exception e) {
            log.error("Failed to process incident notification for: {}", event.incidentId(), e);
        }
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(SlaBreachedEvent event) {
        // Handle urgent SLA breach notifications
        notificationService.sendUrgentAlert(event);
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ChangeApprovedEvent event) {
        // Notify stakeholders of approved changes
        notificationService.sendApprovalNotification(event);
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(UserAssignedToTeamEvent event) {
        // Handle team assignment notifications via event
        notificationService.sendTeamAssignmentNotification(event);
    }
}
```

**Change↔CMDB Impact Assessment Events**:
```java
// Event contracts for impact assessment handshake
public record ChangeImpactAssessmentRequestedEvent(
    String changeId,
    List<String> affectedCiIds,
    String riskLevel,
    Instant requestedAt,
    String version
) {}

public record ChangeImpactAssessedEvent(
    String changeId,
    String impactSummary,
    BigDecimal riskScore,
    List<String> blockers,
    Instant assessedAt,
    String version
) {}

// Publisher in Change module
@Service
class ChangeService {
    private final ApplicationEventPublisher events;
    void requestImpactAssessment(Change change) {
        events.publishEvent(new ChangeImpactAssessmentRequestedEvent(
            change.getId(), change.getAffectedCiIds(), change.getRiskLevel(), Instant.now(), "1.0"
        ));
    }
}

// Listener in CMDB module
@Component
class CmdbImpactHandler {
    private final ApplicationEventPublisher events;
    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(ChangeImpactAssessmentRequestedEvent event) {
        var result = assessImpact(event.affectedCiIds(), event.riskLevel());
        events.publishEvent(new ChangeImpactAssessedEvent(
            event.changeId(), result.summary(), result.riskScore(), result.blockers(), Instant.now(), event.version()
        ));
    }
}
```

**Cross-Module Integration Events**:
- `IncidentCreatedEvent` → Triggers SLA timers, team notifications, knowledge suggestions
- `ChangeApprovedEvent` → Updates CMDB, schedules deployment, notifies teams
- `SlaBreachedEvent` → Escalates incident, notifies management, updates metrics
- `ProblemResolvedEvent` → Updates known errors, closes related incidents
- `UserCreatedEvent` → Provisions access, creates team assignments
- `ChangeImpactAssessedEvent` → CMDB provides impact results to Change Management for approval gating

**Event Design Patterns**:
```java
// Domain event with proper payload design and routing
@Externalized("itsm.incident.created::#{#this.getTeamId()}")
public record IncidentCreatedEvent(
    String incidentId,
    String teamId,
    String severity,
    String affectedServiceId,
    Instant createdAt,
    String version // Event schema version for evolution
) {
    // Getter for routing key
    public String getTeamId() {
        return teamId;
    }
}

// Event listener with proper transaction boundaries and error handling
@Component
public class ProblemEventHandler {
    
    private final ProblemAnalysisService problemAnalysisService;
    
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(IncidentCreatedEvent event) {
        try {
            // Process in separate transaction for reliability
            problemAnalysisService.analyzeIncidentPattern(event);
        } catch (Exception e) {
            // Failed event processing logged but doesn't affect original transaction
            log.error("Failed to analyze incident pattern for incident: {}", event.incidentId(), e);
        }
    }
    
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(IncidentResolvedEvent event) {
        // Update problem correlation when incidents are resolved
        problemAnalysisService.updateProblemCorrelation(event);
    }
}
```

**Event Externalization Configuration**:
```java
@Configuration
@ConditionalOnProperty("spring.modulith.events.externalization.enabled")
public class EventExternalizationConfig {
    
    @Bean
    EventExternalizationConfiguration eventConfig() {
        return EventExternalizationConfiguration.externalizing()
            .select(EventExternalizationConfiguration.annotatedAsExternalized())
            .mapping(IncidentCreatedEvent.class, event -> "incident-created")
            .mapping(ChangeApprovedEvent.class, event -> "change-approved") 
            .mapping(ProblemCreatedEvent.class, event -> "problem-created")
            .routeKey(IncidentCreatedEvent.class, IncidentCreatedEvent::getTeamId)
            .routeKey(ChangeApprovedEvent.class, event -> event.getChangeType())
            .routeKey(ProblemCreatedEvent.class, event -> event.getImpactScope())
            .headers(event -> Map.of(
                "source", "synergyflow-platform",
                "version", getEventVersion(event),
                "correlation-id", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString()
            ))
            .build();
    }
    
    private String getEventVersion(Object event) {
        // Extract version from event or default
        if (event instanceof VersionedEvent versioned) {
            return versioned.getVersion();
        }
        return "1.0";
    }
}

**Event Publication Registry**:
```java
// Enable reliable event publication with Spring Modulith
@Modulithic(
    systemName = "SynergyFlow",
    sharedModules = {"user"}  // Minimize shared modules to reduce coupling
)
@EnablePersistentDomainEvents
@SpringBootApplication
public class SynergyFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SynergyFlowApplication.class, args);
    }
    
    // Custom clock for event publication timestamping
    @Bean
    Clock eventPublicationClock() {
        return Clock.systemUTC();
    }
}

// Event publication management for production scaling
@Configuration
public class EventPublicationConfig {
    
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupCompletedEvents(CompletedEventPublications publications) {
        publications.purgeOlderThan(Duration.ofDays(7));
    }
    
    @EventListener
    public void handleFailedEventPublication(ApplicationFailedEvent event) {
        // Resubmit failed events on application restart
        incompleteEventPublications.resubmitOlderThan(Duration.ofMinutes(30));
    }
}
```
- **JDBC-based Event Store**: Automatic event publication persistence with performance optimization
- **Reliable Delivery**: Failed event handlers automatically retried on restart
- **Event Completion Tracking**: Publications marked complete after successful processing  
- **Transaction-aware Publishing**: Events published only after successful transaction commit
- **Event History**: Audit trail of all published events with timestamps, retained per policy (archived to MinIO)
- **Production Cleanup**: Automated cleanup of completed events to maintain performance
- **Event Replay**: Capability to resubmit failed events for reliability


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
