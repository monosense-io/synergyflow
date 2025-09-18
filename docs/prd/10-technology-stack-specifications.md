---
id: 10-technology-stack-specifications
title: 10. Technology Stack Specifications
version: 8.0
last_updated: 2025-09-03
owner: Architect
status: Draft
---

## 10. Technology Stack Specifications

### 10.1 Backend Technology Stack

**Core Framework**:

- **Spring Boot 3.2+**: Main application framework with `@Modulithic` annotation
- **Spring Modulith BOM**: Dependency management for consistent versions across all modules
- **Build Tool**: Gradle 8.x (Kotlin DSL) with `io.spring.dependency-management` plugin
- **Language**: Java (JDK 21); Kotlin used only for Gradle build scripts

```kotlin
// build.gradle.kts (Gradle Kotlin DSL)
plugins {
  id("org.springframework.boot") version "3.2.x"
  id("io.spring.dependency-management") version "1.1.6"
  java
}

val springModulithVersion: String by project // define in gradle.properties

dependencies {
  implementation(platform("org.springframework.modulith:spring-modulith-bom:$springModulithVersion"))
  // See dependency block below for modules
}
```

- **Spring Modulith Core Dependencies**: Essential modules for production deployment
  - `spring-modulith-events-jdbc`: JDBC-based event publication registry (outbox)
  - `spring-modulith-starter-insight`: Actuator and observability support (runtime scope)
  - `spring-modulith-starter-test`: Module isolation testing with `@ApplicationModuleTest` (test scope)
  - `spring-modulith-observability`: Micrometer tracing for module interactions (runtime scope)
  - `spring-modulith-events-api`: Event publication management APIs (runtime scope)
  - `spring-modulith-events-jackson`: JSON event serialization support (runtime scope)
  - `spring-modulith-events-kafka`: Event externalization to Kafka (runtime scope)
  - `spring-modulith-docs`: Documentation generation (test scope)
  - `spring-boot-starter-actuator`: Required for actuator endpoints (runtime scope)
- **Additional Spring Modules**:
  - **MyBatis-Plus**: Database access via `mybatis-plus-boot-starter`
  - **Spring Security**: Authentication and authorization  
  - **Spring Cache**: Caching abstraction layer
  - **Flyway**: Database migration management
  - **Micrometer**: Application metrics and observability (included in insight starter)

**Spring Modulith Configuration Properties**:

```properties
# Event Publication Registry Performance (Production Optimized)
spring.modulith.events.completion-mode=archive
spring.modulith.events.republish-outstanding-events-on-restart=true
spring.modulith.events.jdbc.schema-initialization.enabled=true

# Connection Pool Optimization for 2000+ TPS
spring.datasource.hikari.maximum-pool-size=150
spring.datasource.hikari.minimum-idle=50
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000

# Event Externalization
spring.modulith.events.externalization.enabled=true
spring.modulith.events.kafka.enable-json=true

# Module Detection Strategy  
spring.modulith.detection-strategy=direct-subpackages

# Testing Configuration
spring.modulith.test.file-modification-detector=uncommitted-changes

# Production Observability
management.endpoints.web.exposure.include=modulith,health,metrics

# Async Processing Configuration
spring.modulith.default-async-termination=true
```

**Database Systems**:

- **PostgreSQL 15+ with High Availability**: Primary database with JSONB support and Spring Modulith optimizations
  - Connection pooling: HikariCP (max 200 connections production, auto-scaling for 2000+ TPS)
  - **CloudNative-PG**: PostgreSQL cluster with high availability and automated failover
  - Extensions: pg_stat_statements, pg_trgm for search, pg_partman for partitioning
  - Partitioning for audit tables, event_publication (by month), and historical data
  - **Event Publication Registry Optimizations**:
    - Daily cleanup jobs for completed events (policy-based retention, archive to MinIO)
    - Completion indexing for performance at 2000+ TPS
    - `completion_date IS NULL` index for active event queries
    - Event publication table partitioning by publication_date
    - Automated archive strategy for historical event analysis
  - **Reliability Features**:
    - Point-in-time recovery (PITR) with 30-day retention
    - Continuous archiving to object storage
    - Regular backup validation and restore testing
- **Redis 7.x (Cluster/Sentinel)**: Distributed in-memory datastore for caching and sessions
  - Session storage with automatic failover
  - Query result caching with cross-node/rack replication
  - Rate limiting counters with distributed consistency
  - Real-time data for dashboards with pub/sub patterns
  - **Event Publication Cache**: Cache for frequent event publication queries
  - **Module State Cache**: Cross-module query result caching with TTL
  - **Notification Preferences**: User and team notification settings cache

**Object Storage**:

- **MinIO**: S3-compatible object storage
  - File attachments up to 100MB
  - Knowledge base documents and images
  - Audit trail document storage
  - Automatic cleanup policies

### 10.2 Frontend Technology Stack (Next.js + shadcn/ui + Tailwind)

**Framework & Tooling**:

- **Next.js 14+ (App Router, RSC)**: React framework with Server Components, streaming, and edge-ready routing
- **TypeScript**: Strict types for safety and DX
- **Tailwind CSS**: Utility-first styling with design tokens via CSS variables
- **shadcn/ui**: Headless, accessible components styled with Tailwind
- **Zod + React Hook Form**: Schema-driven, type-safe forms
- **TanStack Query**: Server-state caching, retries, background sync
- **Redux Toolkit or Zustand**: Local/UI state management as needed

**Design System Architecture (ADS‑Inspired, JIRA‑like)**:

- **Token-Driven Foundation**: CSS variables and Tailwind theme tokens inspired by Atlassian DS semantics
- **Component Library**: shadcn/ui primitives extended to JIRA‑like patterns (tables, boards, modals)
- **Theming**: Light/Dark themes with runtime switching; WCAG 2.1 AA contrast
- **Spacing & Typography**: 4px scale; font scales mapped to ADS h100–h900 equivalents
- **Icons**: lucide-react by default; support custom SVG packs
- **Build Pipeline**: Style Dictionary (tokens → CSS variables/Tailwind config) with lint checks

**Enterprise UI Patterns**:

- **Data Tables**: DataGrid (shadcn/ui Table + virtualization) with inline edit, bulk actions, export
- **Form Wizards**: Multi-step with conditional logic and async validation
- **Dashboards**: Grid layout with drag/drop widgets; server-side data hydration
- **Navigation**: Breadcrumbs, left-nav, and contextual actions in JIRA‑like layout

**App Architecture**:

- **Next App Router**: Route groups, parallel routes, and layouts for feature areas
- **Server Components**: Fetch server-side with streaming; client components where interactivity is needed
- **API Access**: Fetch/axios with interceptors; TanStack Query for caching/invalidation
- **Auth**: NextAuth.js (OIDC/SAML via proxy) with RBAC-aware guards
- **i18n**: next-intl or lingui; RTL support

**Accessibility**:

- Keyboard focus management, ARIA attributes, and screen-reader labels
- Color contrast validated against WCAG 2.1 AA

### 10.3 Flutter Cross-Platform Application (Optimized for Enterprise)

**Objective**: Single cross-platform app for on-call agents and field technicians

**Platform Requirements**:

- **Flutter 3.x**: Google's enterprise-ready UI toolkit in "Production Era"
- **Target Platforms**: iOS 15+ and Android API 26+ from single codebase
- **Architecture**: Shared business logic with platform-adaptive UI
- **Enterprise Benefits**: 75% cost reduction vs dual native maintenance

**Flutter Enterprise Advantages**:

- **Google Backing**: Strategic investment with automotive enterprise adoption (BMW, Toyota)
- **Performance**: 60-120 FPS rendering with native compilation (ARM/x86)
- **Single Codebase**: Reduced development and maintenance costs
- **Rapid Iteration**: Hot reload for faster development cycles
- **Platform Integration**: Native modules for device-specific features

**Core Mobile Features (Flutter Implementation)**:

- **Offline Capability**: Local SQLite database with sync conflict resolution
- **Push Notifications**: Firebase Cloud Messaging for iOS and Android
- **Biometric Authentication**: Platform-adaptive Face ID, Touch ID, fingerprint
- **Camera Integration**: Native camera access for incident documentation
- **Location Services**: GPS integration for field service requests
- **Audio Recording**: Platform-optimized voice notes for incident updates
- **Platform Channels**: Direct access to native APIs when needed
- **Shared State Management**: Riverpod for consistent state across platforms

**On-Call Optimization**:

- **Quick Actions**: One-tap incident acknowledgment and status updates
- **Emergency Escalation**: Direct calling and messaging from incident details
- **Offline Mode**: Critical ticket information cached locally
- **Sync Resolution**: Automatic conflict resolution when connectivity restored
- **Dark Mode**: Optimized for late-night on-call scenarios

**Spring Modulith Integration**:

```java
@ApplicationModule
package io.monosense.synergyflow.mobile;

@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class MobileApiController {
    
    private final IncidentService incidentService;
    private final ApplicationEventPublisher events;
    
    @PostMapping("/incidents/{id}/quick-update")
    public ResponseEntity<Void> quickIncidentUpdate(
            @PathVariable String id,
            @RequestBody MobileIncidentUpdateRequest request) {
        
        // Optimized mobile API for quick status updates
        incidentService.updateIncidentMobile(id, request);
        
        // Publish mobile interaction event for analytics
        events.publishEvent(new MobileInteractionEvent(
            request.getUserId(), "incident_update", id, Instant.now()
        ));
        
        return ResponseEntity.ok().build();
    }
}
```

**Acceptance Criteria**:

- Native app performance with <2 second launch time
- Offline functionality for critical ticket operations
- Push notification delivery rate >95% for iOS and Android
- Biometric authentication integration with existing security model
- Sync conflict resolution preserves data integrity
- Battery optimization for extended on-call usage

### 10.4 Comprehensive Monitoring & Observability Stack

**Objective**: Enterprise-grade observability for 99.5% availability and proactive issue detection

**Core Monitoring Infrastructure**:

- **Prometheus**: Metrics collection and alerting for on-premise Kubernetes deployment
  - Application metrics: Response times, error rates, throughput
  - Infrastructure metrics: CPU, memory, disk, network utilization  
  - Business metrics: SLA compliance, ticket volumes, user satisfaction
  - Spring Modulith metrics: Module interactions, event publication performance

- **Grafana**: Visualization and dashboarding for internal monitoring
  - Real-time operational dashboards with drill-down capabilities
  - Executive dashboards with KPI summaries and trend analysis
  - SLA monitoring with traffic light indicators and breach alerts
  - Module dependency visualization and health monitoring

- **OpenTelemetry**: Distributed tracing and instrumentation
  - End-to-end request tracing across Spring Modulith modules
  - Cross-module event correlation and performance analysis
  - API performance monitoring with detailed latency breakdown
  - Database query tracing and optimization insights

**Centralized Logging**:

- **Elasticsearch + Logstash + Kibana (ELK Stack)**:
  - Centralized log aggregation from all application components
  - Structured logging with correlation IDs for request tracing
  - Log-based alerting for error patterns and security events
  - Audit trail storage with tamper-proof logging

- **Log Management Features**:
  - 30-day retention for operational logs, 7-year for audit logs
  - Real-time log streaming for incident response
  - Automated log parsing and indexing for quick searches
  - Integration with Spring Modulith event publication registry

**Alerting & Incident Response**:

- **AlertManager**: Intelligent alert routing and escalation
  - SLA breach notifications with automated escalation workflows
  - Module boundary violation alerts for architectural compliance
  - Performance degradation warnings with predictive thresholds
  - Security incident alerts with SIEM integration

- **Alert Integration**:
  - Telegram notifications for immediate incident response
  - Email notifications with HTML templates and priority levels
  - Integration with existing monitoring systems (Nagios, Zabbix)
  - Webhook support for external incident management systems

**Application Performance Monitoring (APM)**:

- **Spring Boot Actuator**: Built-in monitoring endpoints
  - `/actuator/modulith`: Runtime module architecture inspection  
  - `/actuator/health`: Application health checks with component details
  - `/actuator/metrics`: JVM and application metrics exposure
  - `/actuator/prometheus`: Metrics export for Prometheus scraping

**Business Intelligence & Analytics**:

- **Event-Driven Analytics**: Real-time analytics from Spring Modulith events
- **Dashboard Automation**: Automated report generation and distribution
- **Predictive Analytics**: Machine learning models for capacity planning
- **Cross-System Correlation**: Integration with external business systems

**Monitoring Architecture Integration**:

```java
@Configuration
public class MonitoringConfig {
    
    @Bean
    MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    // Spring Modulith event monitoring
    @EventListener
    public void onModuleEvent(ApplicationModuleEvent event) {
        meterRegistry.counter("module.event", 
            "source", event.getSource(),
            "target", event.getTarget(),
            "type", event.getType()).increment();
    }
}
```

**Compliance & Security Monitoring**:

- **GDPR Compliance**: Data access logging and retention management
- **SOX Compliance**: Financial change approval audit trails
- **Security Monitoring**: Failed authentication attempts and privilege escalation detection
- **Module Security**: Cross-module access control and boundary violation monitoring

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
