---
id: 20-deployment-operations
title: 20. Deployment & Operations
version: 8.0
last_updated: 2025-09-03
owner: Infra/Platform
status: Draft
---

## 20. Deployment & Operations

### 20.1 Deployment Architecture

**Production Environment**:

- Application server: 4 CPU cores, 8GB RAM minimum
- Database server: 8 CPU cores, 16GB RAM, SSD storage
- Cache server: 2 CPU cores, 4GB RAM
- Object storage: 1TB initial capacity with auto-scaling

**Development Environment**:

- Docker Compose setup for local development
- H2 database for unit testing
- Test data generators for development workflows
- CI/CD pipeline with automated testing

### 20.2 Monitoring & Observability

**Spring Modulith Actuator Integration**:

```properties
# Enable modulith actuator endpoint
management.endpoints.web.exposure.include=modulith,health,metrics,info
management.endpoint.modulith.enabled=true
management.endpoint.health.show-details=always
```

**Module Boundary Monitoring**:

```java
@Component
@RequiredArgsConstructor
public class ModuleHealthMonitor {
    
    private final ApplicationModules modules;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void validateModuleBoundaries() {
        try {
            modules.verify();
            log.info("Module boundary validation passed");
        } catch (Violations violations) {
            log.error("Module boundary violations detected: {}", violations);
            alertService.sendArchitecturalAlert(violations);
        }
    }
    
    @EventListener
    public void onModuleInteraction(ModuleInteractionEvent event) {
        metricsRegistry.counter("module.interaction", 
            "source", event.getSource(),
            "target", event.getTarget()).increment();
    }
}
```

**Application Monitoring**:

- Spring Boot Actuator endpoints with module health checks
- Micrometer metrics collection with module interaction tracking
- Distributed tracing with correlation IDs and module boundaries
- Custom business metrics dashboards with module performance
- `/actuator/modulith` endpoint for runtime architecture inspection

**Alerting Strategy**:

- SLA breach notifications with module context
- System health alerts including module dependency failures
- Security incident notifications with module boundary violations
- Performance degradation warnings with module-specific metrics
- Architectural compliance alerts for module boundary violations

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
