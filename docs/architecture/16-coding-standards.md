# 16. Coding Standards

> **🚨 CRITICAL MANDATORY REQUIREMENTS - NON-NEGOTIABLE:**
>
> The following standards are **MANDATORY** for all code contributions. Pull requests that violate these requirements will be **REJECTED** during code review:
>
> 1. **Lombok** - ALL DTOs, Entities, Services MUST use Lombok annotations (`@Value`, `@Data`, `@RequiredArgsConstructor`, `@Slf4j`)
> 2. **MapStruct** - ALL DTO ↔ Entity mappings MUST use MapStruct with strict configuration (`unmappedTargetPolicy = ERROR`)
> 3. **Javadoc** - ALL public classes and methods MUST have comprehensive Javadoc with `@author monosense`, `@since`, `@param`, `@return`, `@throws`
> 4. **Testcontainers** - ALL integration tests MUST use Testcontainers PostgreSQL (H2/in-memory databases are FORBIDDEN)
> 5. **Package Naming** - ALL Java packages MUST start with `io.monosense.synergyflow.*`
> 6. **Spring Modulith** - ALL module boundaries MUST be enforced (entities/repositories in `.internal` package only)
>
> **Enforcement:** Automated CI checks + mandatory code review verification

---

## 16.1 Critical Fullstack Rules

**Backend (Java/Spring Boot):**
- **Module Boundaries:** Never import classes across Spring Modulith modules except via public APIs or events
- **Event Publishing:** Always publish events in @Transactional methods (atomic with aggregate write)
- **Correlation IDs:** Always propagate correlationId from MDC to events and logs
- **Repository Pattern:** Use Spring Data JPA repositories (JpaRepository), never direct EntityManager unless necessary
- **DTO Mapping:** Never expose JPA entities directly in REST API responses (use DTOs)

**Frontend (TypeScript/Next.js):**
- **Server vs Client Components:** Default to Server Components, use 'use client' only when necessary (interactivity, hooks)
- **API Calls:** All API calls go through lib/api services, never direct fetch() in components
- **Type Safety:** Always import types from types/ directory (generated from backend DTOs)
- **State Management:** Server state in TanStack Query, client-only state in Zustand (never mix)
- **Environment Variables:** Access via process.env.NEXT_PUBLIC_* for client-side, process.env.* for server-side

**Cross-Stack:**
- **Correlation IDs:** Frontend generates UUID correlation ID, passes in X-Correlation-ID header, backend propagates to events/logs
- **Error Handling:** Frontend catches ApiError and displays user-friendly messages, backend returns RFC 7807 Problem Details
- **Naming Conventions:** Consistent naming across stack (incidentId in DB/Java/TS, not id/incident_id mix)

## 16.2 Java Tooling (MANDATORY: Lombok, MapStruct)

> **⚠️ MANDATORY REQUIREMENT:** All backend Java code MUST use Lombok and MapStruct. Code reviews will reject PRs that do not follow these standards.

**Lombok - MANDATORY for Boilerplate Reduction:**
- **DTOs (Public API):** Use `@Value` for immutable DTOs at module root (e.g., `IncidentDto.java`). Generates all-args constructor, getters, equals/hashCode, toString.
- **Entities (Internal):** Use `@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder` for JPA entities in `.internal` package. `@Data` provides getters/setters, equals/hashCode (ID-based recommended).
- **Services (Internal):** Use `@RequiredArgsConstructor` for constructor injection + `@Slf4j` for logging in service implementations.
- **Events (Public API):** Use Java `record` types for domain events (immutable by default, no Lombok needed).
- **Configuration:** Place `lombok.config` in `backend/` root with `lombok.addLombokGeneratedAnnotation = true` for test coverage exclusion.

**MapStruct - MANDATORY for Type-Safe DTO ↔ Entity Mapping:**
- **Mapper Interface:** Define package-private mappers in `.internal` package with:
  ```java
  @Mapper(
      componentModel = "spring",
      injectionStrategy = InjectionStrategy.CONSTRUCTOR,
      unmappedTargetPolicy = ReportingPolicy.ERROR,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
  )
  interface IncidentMapper { /* ... */ }
  ```
- **Mapping Direction:** `Entity → DTO` (expose to other modules), `DTO → Entity` (create), `DTO + @MappingTarget Entity` (update).
- **Custom Logic:** Use `default` methods for complex conversions (e.g., SLA deadline calculation, enum mapping).
- **Package Placement:** Mappers live in `.internal` package alongside entities (e.g., `io.monosense.synergyflow.incident.internal.IncidentMapper`).
- **Lombok Integration:** Add `lombok-mapstruct-binding:0.2.0` to `annotationProcessor` dependencies for Lombok + MapStruct interoperability.
- **Validation:** `unmappedTargetPolicy = ReportingPolicy.ERROR` ensures compile-time detection of unmapped fields.

**Package Boundary Enforcement (Spring Modulith):**
- **Public API (Module Root):** DTOs, Service interfaces, Events (visible to other modules)
- **Internal (`.internal` package):** Entities, Repositories, Service implementations, Mappers (NOT visible to other modules)
- **No Entity Leakage:** Never expose JPA entities across module boundaries or in REST API responses. Always use DTOs mapped via MapStruct.

## 16.3 Testing Standards

**Unit Tests (JUnit 5 + Mockito):**
- **Scope:** Service logic, mappers (MapStruct), domain event listeners, utility classes
- **Framework:** JUnit 5 (`@ExtendWith(MockitoExtension.class)`) + Mockito for mocking dependencies
- **Coverage Target:** 80% line coverage for service layer, 100% for critical business logic (SLA calculations, policy decisions)
- **Test Organization:** Mirror production package structure in `src/test/java/io/monosense/synergyflow/`
- **Example:** `IncidentServiceImplTest.java` tests `IncidentServiceImpl` with mocked repository and event publisher

**Integration Tests (Spring Boot Test Slices):**
- **Web Layer:** Use `@WebMvcTest` for REST controller tests with mocked services (no database)
- **Persistence Layer:** Use `@DataJpaTest` for repository tests with real database (Testcontainers PostgreSQL)
- **Full Integration:** Use `@SpringBootTest` with Testcontainers for end-to-end module tests (API → Service → Repository → Database)

**Database Testing - MANDATORY Testcontainers:**
- **FORBIDDEN:** Never use H2, HSQLDB, or any in-memory database for tests. PostgreSQL-specific features (JSONB, full-text search) will not work correctly.
- **REQUIRED:** Always use Testcontainers with PostgreSQL 16.8 image for integration tests:
  ```java
  @Testcontainers
  @SpringBootTest
  class IncidentModuleIntegrationTest {
      @Container
      @ServiceConnection  // Spring Boot 3.1+ auto-wiring
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine");
  }
  ```
- **MinIO for File Storage:** Include Testcontainers MinIO when testing attachment/audit log storage features
- **Performance:** Reuse containers across test classes with `@Container(reuse = true)` to reduce startup overhead

**End-to-End Tests (Playwright):**
- **Scope:** Critical user journeys (incident creation, change approval, knowledge search)
- **Framework:** Playwright 1.40+ with TypeScript
- **Target:** Cross-browser testing (Chromium, Firefox, WebKit)
- **CI Integration:** Run on every PR, generate trace artifacts on failure

**Performance Tests (k6):**
- **Scope:** Critical flows (incident creation, ticket routing, knowledge article search)
- **Framework:** k6 scripts with defined thresholds (response time p95 < 500ms, error rate < 1%)
- **Reference:** See ADR-0031 for performance testing strategy and acceptance criteria
- **CI Integration:** Run nightly on staging environment, fail build if thresholds breached

## 16.4 Javadoc Requirements (MANDATORY)

> **⚠️ MANDATORY REQUIREMENT:** All public classes, interfaces, and methods MUST have comprehensive Javadoc. Code reviews will reject PRs without proper documentation.
>
> **🎯 AUDIENCE:** Write Javadoc for **junior developers** who may be unfamiliar with the codebase. Assume the reader:
> - Has basic Java knowledge but limited Spring Boot/Spring Modulith experience
> - Needs context about WHY code exists, not just WHAT it does
> - Benefits from real-world examples and common pitfalls
> - Requires explanations of business domain concepts (SLA, BPMN, OPA, etc.)
>
> **PRINCIPLE:** If a junior developer cannot understand your Javadoc and use the API correctly without reading the implementation, the documentation is INSUFFICIENT.

**Class-Level Documentation (REQUIRED for ALL classes):**
```java
/**
 * Service implementation for managing incidents with SLA tracking and policy-driven routing.
 *
 * <p>This service handles the complete incident lifecycle from creation to resolution,
 * including automatic SLA timer initialization via Flowable BPMN workflows and
 * policy-based agent assignment via OPA sidecar evaluation.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Transactional incident creation with domain event publishing</li>
 *   <li>Automatic SLA deadline calculation based on priority (Critical: 4h, High: 8h, Medium: 24h, Low: 72h)</li>
 *   <li>Optimistic locking for concurrent modification prevention</li>
 *   <li>Idempotent event listeners with processed_events table</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class MyService {
 *     private final IncidentService incidentService;
 *
 *     public void handleCustomerIssue(String title, String description) {
 *         IncidentDto dto = new IncidentDto(
 *             null, title, description,
 *             Priority.HIGH, Severity.S2, null, null, userId,
 *             null, null, null, null, null, null
 *         );
 *         IncidentDto created = incidentService.createIncident(dto);
 *         // SLA timer automatically started via IncidentCreatedEvent
 *     }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see IncidentDto
 * @see IncidentCreatedEvent
 */
@Service
@RequiredArgsConstructor
@Slf4j
class IncidentServiceImpl implements IncidentService {
    // Implementation...
}
```

**Method-Level Documentation (REQUIRED for ALL public methods):**
```java
/**
 * Creates a new incident and publishes {@link IncidentCreatedEvent} atomically.
 *
 * <p>The incident is persisted with status {@code NEW}, SLA deadline calculated
 * based on priority, and a domain event published via Spring Modulith's transactional
 * outbox pattern. Event listeners (e.g., {@code SLATimerListener}) will asynchronously
 * start Flowable BPMN timer workflows.</p>
 *
 * <p><strong>SLA Deadlines:</strong></p>
 * <ul>
 *   <li>CRITICAL: 4 hours from creation</li>
 *   <li>HIGH: 8 hours from creation</li>
 *   <li>MEDIUM: 24 hours from creation</li>
 *   <li>LOW: 72 hours from creation</li>
 * </ul>
 *
 * @param dto incident data without ID (ID generated by database)
 * @return created incident with generated ID, audit timestamps, and SLA deadline
 * @throws IllegalArgumentException if dto is null or validation fails
 * @throws DataIntegrityViolationException if duplicate incident detected
 */
@Override
@Transactional
public IncidentDto createIncident(IncidentDto dto) {
    // Implementation...
}
```

**Javadoc Writing Standards (Junior Developer Friendly):**

**1. Class-Level Requirements:**
- **REQUIRED Tags:** `@author monosense`, `@since <version>`, `@version <current-version>`
- **First Paragraph:** One-sentence summary in plain language (what the class does)
- **Second Paragraph:** Detailed explanation of purpose and responsibilities (why it exists)
- **Key Features List:** Bullet points of major capabilities with business context
- **Architecture Context:** Explain how this class fits into Spring Modulith module boundaries
- **Usage Example:** Complete working example showing typical use case
- **Thread Safety:** Document if class is thread-safe or requires synchronization
- **Related Classes:** Use `@see` tags to link to DTOs, events, repositories, configuration

**2. Method-Level Requirements:**
- **REQUIRED Tags:** `@param`, `@return`, `@throws` where applicable (NO `@author`/`@version`)
- **First Paragraph:** Plain language description of what the method does and when to use it
- **Business Context:** Explain business rules and domain concepts (e.g., "SLA deadline is calculated based on incident priority per ITIL standards")
- **Parameter Details:** For each `@param`, explain:
  - What the parameter represents in business terms
  - Valid values/ranges/formats
  - What happens if null or invalid (even if validation exists)
- **Return Value Details:** For `@return`, explain:
  - What the return value represents
  - Possible states (never null? empty list vs null?)
  - How to interpret the result
- **Exception Details:** For each `@throws`, explain:
  - WHEN the exception is thrown (specific conditions)
  - WHY it's thrown (business rule violation? technical issue?)
  - HOW the caller should handle it
- **Side Effects:** Document if method publishes events, modifies database, calls external APIs
- **Transactional Behavior:** Document if method must be called within transaction

**3. Explain Domain Concepts:**
- **SLA (Service Level Agreement):** "The maximum time allowed to resolve an incident before breach"
- **BPMN (Business Process Model Notation):** "Workflow diagrams executed by Flowable engine"
- **OPA (Open Policy Agent):** "External policy engine that evaluates authorization rules"
- **Correlation ID:** "Unique identifier propagated across events/logs for distributed tracing"
- **Transactional Outbox:** "Pattern ensuring events are published atomically with database writes"
- **Optimistic Locking:** "Concurrency control using version numbers to detect conflicts"

**4. Include Common Pitfalls:**
```java
/**
 * Assigns incident to specified agent.
 *
 * <p><strong>⚠️ IMPORTANT:</strong> This method uses optimistic locking. If another
 * transaction modifies the same incident between read and write, an
 * {@link OptimisticLockingFailureException} will be thrown. Callers should catch
 * this exception and retry the operation with fresh data.</p>
 *
 * <p><strong>Common Pitfall:</strong> Do NOT call this method in a loop without
 * handling optimistic lock exceptions. Use exponential backoff retry strategy.</p>
 *
 * @param id incident UUID (must exist in database)
 * @param assignedTo agent UUID (must be valid user with AGENT role)
 * @return updated incident with new assignment and ASSIGNED status
 * @throws ResourceNotFoundException if incident with given ID does not exist
 * @throws OptimisticLockingFailureException if incident was modified by another transaction
 * @throws IllegalArgumentException if assignedTo user does not have AGENT role
 */
```

**5. Real-World Examples:**
```java
/**
 * <p><strong>Usage Example (Typical Case):</strong></p>
 * <pre>{@code
 * // Create incident and let system automatically start SLA timer
 * IncidentDto newIncident = new IncidentDto(
 *     null,                          // ID auto-generated
 *     "Database connection timeout", // Title
 *     "Production API returning 503", // Description
 *     Priority.CRITICAL,             // Sets 4-hour SLA deadline
 *     Severity.S1,
 *     Status.NEW,
 *     null,                          // Not yet assigned
 *     currentUserId,
 *     null, null, null, null, null, null
 * );
 * IncidentDto created = incidentService.createIncident(newIncident);
 * // At this point:
 * // - Incident saved to database with status=NEW
 * // - IncidentCreatedEvent published to event_publication table
 * // - SLATimerListener will asynchronously start Flowable workflow
 * // - SLA timer will fire alert 30 minutes before 4-hour deadline
 * }</pre>
 *
 * <p><strong>Usage Example (Edge Case - Handling Optimistic Lock):</strong></p>
 * <pre>{@code
 * // Retry logic for concurrent modification
 * int maxRetries = 3;
 * for (int attempt = 0; attempt < maxRetries; attempt++) {
 *     try {
 *         IncidentDto updated = incidentService.assignIncident(incidentId, agentId);
 *         break; // Success, exit loop
 *     } catch (OptimisticLockingFailureException e) {
 *         if (attempt == maxRetries - 1) throw e; // Give up after 3 attempts
 *         Thread.sleep(100 * (attempt + 1)); // Exponential backoff
 *     }
 * }
 * }</pre>
 */
```

**6. Documentation Quality Checklist:**
- [ ] Can a junior developer understand the purpose without reading implementation?
- [ ] Are all domain-specific terms explained (SLA, BPMN, OPA, etc.)?
- [ ] Does the example show both happy path AND error handling?
- [ ] Are business rules explicitly stated (e.g., "CRITICAL priority = 4 hour SLA")?
- [ ] Are side effects documented (event publishing, database writes, API calls)?
- [ ] Are concurrency concerns explained (thread safety, optimistic locking)?
- [ ] Are null handling semantics clear (can parameters be null? return value?)?
- [ ] Are exception conditions specific (not just "if something goes wrong")?

**7. HTML Formatting for Readability:**
- Use `<p>` for paragraph breaks (improves readability)
- Use `<ul>` and `<li>` for lists (better than comma-separated)
- Use `<strong>` for emphasis (IMPORTANT, WARNING, NOTE)
- Use `<code>` for inline code references (variable names, class names)
- Use `<pre>{@code ... }</pre>` for multi-line code examples
- Use `{@link ClassName}` for cross-references to other classes
- Use `@see` tags at the end to link related documentation

**8. Cross-References (Required):**
- **DTOs:** Link to request/response DTOs used by the method
- **Events:** Link to domain events published or consumed
- **Exceptions:** Link to custom exception classes
- **Related Services:** Link to services called by this class
- **Architecture Docs:** Reference ADRs or architecture.md sections where applicable

---

### **BAD vs GOOD Javadoc Examples**

**❌ BAD - Insufficient for Junior Developers:**
```java
/**
 * Creates an incident.
 *
 * @param dto the incident
 * @return created incident
 * @throws Exception if error occurs
 */
@Transactional
public IncidentDto createIncident(IncidentDto dto) {
    // Implementation...
}
```

**Problems:**
- ❌ No business context (what is an incident? why create one?)
- ❌ No explanation of SLA timer or event publishing
- ❌ Vague parameter description ("the incident")
- ❌ Generic exception (which exception? when?)
- ❌ No usage example
- ❌ No mention of side effects (database write, event publish)
- ❌ Junior dev doesn't know SLA timer starts automatically

---

**✅ GOOD - Junior Developer Friendly:**
```java
/**
 * Creates a new incident in the system and automatically initiates SLA tracking.
 *
 * <p>This method is the entry point for all incident creation workflows. When an incident
 * is created, the system performs several automated actions:</p>
 * <ol>
 *   <li>Saves the incident to the database with status {@code NEW}</li>
 *   <li>Calculates SLA deadline based on priority (CRITICAL=4h, HIGH=8h, MEDIUM=24h, LOW=72h)</li>
 *   <li>Publishes {@link IncidentCreatedEvent} via Spring Modulith's transactional outbox</li>
 *   <li>Event listeners asynchronously start Flowable BPMN timer for SLA monitoring</li>
 * </ol>
 *
 * <p><strong>Business Context:</strong> Incidents represent IT service disruptions reported
 * by end users. The SLA (Service Level Agreement) defines maximum resolution time based on
 * business impact (priority) and technical severity. Breaching SLA triggers escalation to
 * management per ITIL best practices.</p>
 *
 * <p><strong>⚠️ IMPORTANT:</strong> This method MUST be called within a transaction because
 * event publishing uses the transactional outbox pattern. The {@link IncidentCreatedEvent}
 * will only be delivered if the database transaction commits successfully. If the transaction
 * rolls back, the event is discarded automatically.</p>
 *
 * <p><strong>Thread Safety:</strong> This method is thread-safe. Multiple concurrent calls
 * will create separate incidents with unique auto-generated UUIDs.</p>
 *
 * <p><strong>Usage Example (Typical Case):</strong></p>
 * <pre>{@code
 * // Example: Customer reports database connection issue
 * IncidentDto newIncident = new IncidentDto(
 *     null,                              // ID will be auto-generated
 *     "Production DB connection timeout", // Brief description of issue
 *     "API endpoints returning 503 errors due to connection pool exhaustion", // Detailed description
 *     IncidentDto.Priority.CRITICAL,      // High business impact = 4-hour SLA
 *     IncidentDto.Severity.S1,            // Production system down
 *     IncidentDto.Status.NEW,             // Initial status
 *     null,                               // Not yet assigned to agent
 *     currentUser.getId(),                // Reporter (authenticated user)
 *     null, null, null, null, null, null  // Audit fields set automatically
 * );
 *
 * IncidentDto created = incidentService.createIncident(newIncident);
 *
 * // At this point:
 * // - Incident saved with generated UUID (e.g., "550e8400-e29b-41d4-a716-446655440000")
 * // - SLA deadline = current time + 4 hours (CRITICAL priority)
 * // - IncidentCreatedEvent in event_publication table (not yet processed)
 * // - Within seconds, SLATimerListener will start Flowable BPMN workflow
 * // - Flowable timer will send alert 30 minutes before SLA breach deadline
 *
 * logger.info("Incident created: ID={}, SLA deadline={}", created.getId(), created.getSlaDeadline());
 * }</pre>
 *
 * <p><strong>Usage Example (Error Handling):</strong></p>
 * <pre>{@code
 * try {
 *     IncidentDto created = incidentService.createIncident(newIncident);
 * } catch (IllegalArgumentException e) {
 *     // Validation failure (e.g., title too short, null priority)
 *     logger.error("Invalid incident data: {}", e.getMessage());
 *     return ResponseEntity.badRequest().body(ProblemDetail.forStatus(400));
 * } catch (DataIntegrityViolationException e) {
 *     // Database constraint violation (rare - should not happen with UUIDs)
 *     logger.error("Database integrity violation", e);
 *     return ResponseEntity.status(409).body(ProblemDetail.forStatus(409));
 * }
 * }</pre>
 *
 * @param dto incident data without ID (ID will be auto-generated by database).
 *            Must include: title (5-200 chars), description (max 5000 chars),
 *            priority (determines SLA deadline), severity (S1-S4), createdBy (reporter UUID).
 *            Optional: assignedTo (null for unassigned incidents).
 *            Do NOT provide: id, createdAt, updatedAt, slaDeadline (auto-calculated).
 * @return created incident with generated ID, audit timestamps (createdAt, updatedAt),
 *         calculated SLA deadline, and status=NEW. Never returns null.
 *         The returned DTO is a snapshot at creation time; subsequent changes by other
 *         transactions will not be reflected in this object.
 * @throws IllegalArgumentException if dto is null, or validation fails (title too short/long,
 *                                  description exceeds max length, priority/severity/status missing)
 * @throws DataIntegrityViolationException if database constraint violated (extremely rare
 *                                         due to UUID auto-generation, but possible if database
 *                                         is corrupted or unique constraints exist)
 * @see IncidentDto the data transfer object representing incident data
 * @see IncidentCreatedEvent domain event published atomically with incident creation
 * @see SLATimerListener event listener that starts Flowable BPMN workflow for SLA tracking
 * @see <a href="../../../../../docs/architecture.md#sla-calculation">SLA Calculation Rules</a>
 */
@Override
@Transactional
public IncidentDto createIncident(IncidentDto dto) {
    // Implementation...
}
```

**Why This is GOOD:**
- ✅ Explains business purpose (IT service disruption tracking)
- ✅ Defines domain terms (SLA, ITIL, transactional outbox, BPMN)
- ✅ Lists all automated actions (4-step process)
- ✅ Shows complete working example with inline comments
- ✅ Demonstrates error handling (try-catch with specific exceptions)
- ✅ Explains SLA deadline calculation rules explicitly
- ✅ Documents side effects (database write, event publish, workflow trigger)
- ✅ Warns about transactional requirements
- ✅ Clarifies thread safety
- ✅ Detailed `@param` (what to include, what NOT to include, valid ranges)
- ✅ Detailed `@return` (never null, snapshot semantics)
- ✅ Specific `@throws` (exact conditions for each exception)
- ✅ Cross-references to DTOs, events, listeners, architecture docs
- ✅ Junior dev can implement caller without reading implementation code

---

## 16.5 Naming Conventions

| Element | Frontend | Backend | Database | Example |
|---------|----------|---------|----------|---------|
| Components | PascalCase | - | - | `IncidentList.tsx` |
| Hooks | camelCase with 'use' prefix | - | - | `useIncidents.ts` |
| API Services | camelCase | - | - | `incidentsApi.list()` |
| REST Controllers | - | PascalCase | - | `IncidentController.java` |
| Services | - | PascalCase | - | `IncidentService.java` |
| JPA Entities | - | PascalCase | - | `Incident.java` |
| API Routes | kebab-case | - | - | `/api/v1/incidents/{id}` |
| Database Tables | - | - | snake_case | `incidents` |
| Database Columns | - | - | snake_case | `assigned_to` |
| Event Names | - | PascalCase with 'Event' suffix | - | `IncidentCreatedEvent` |
| Java Packages | - | lowercase with dots | - | `io.monosense.synergyflow.incident` |

---

## 16.6 Enforcement & Code Review Checklist

**Automated CI/CD Checks (MUST PASS before merge):**
- ✅ **Compilation Check:** All Lombok and MapStruct code must compile successfully
- ✅ **Test Coverage:** Minimum 80% line coverage on service layer (JaCoCo report)
- ✅ **Javadoc Generation:** `./gradlew javadoc` must succeed without warnings
- ✅ **Testcontainers:** No H2/HSQLDB dependencies in `build.gradle.kts`
- ✅ **Checkstyle/PMD:** No violations of Lombok/MapStruct usage patterns
- ✅ **Spring Modulith Verification:** `./gradlew test` includes `ApplicationModulesTest` (module boundary validation)

**Mandatory Code Review Checklist (Reviewers MUST verify):**

**Lombok Compliance:**
- [ ] DTOs use `@Value` (immutable)
- [ ] Entities use `@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder`
- [ ] Services use `@RequiredArgsConstructor` + `@Slf4j`
- [ ] No manual getters/setters/constructors (Lombok generates them)
- [ ] `lombok.config` present in `backend/` root

**MapStruct Compliance:**
- [ ] All DTO ↔ Entity mappings use MapStruct mappers
- [ ] Mappers in `.internal` package with `@Mapper(componentModel = "spring")`
- [ ] `unmappedTargetPolicy = ReportingPolicy.ERROR` configured
- [ ] No manual mapping code (e.g., `new IncidentDto(entity.getId(), entity.getTitle(), ...)`)
- [ ] Lombok + MapStruct integration dependency present (`lombok-mapstruct-binding:0.2.0`)

**Javadoc Compliance (Junior Developer Friendly):**
- [ ] All public classes have class-level Javadoc with `@author monosense`, `@since`, `@version`
- [ ] All public methods have method-level Javadoc with `@param`, `@return`, `@throws`
- [ ] **Plain Language:** Documentation uses clear, simple language (no unexplained jargon)
- [ ] **Domain Concepts Explained:** Terms like SLA, BPMN, OPA, Correlation ID, Transactional Outbox are defined
- [ ] **Business Context:** Why the code exists and what business problem it solves is explained
- [ ] **Complete Examples:** Working code examples show both happy path AND error handling
- [ ] **Common Pitfalls:** Warnings about threading, optimistic locking, null handling included
- [ ] **Side Effects:** Events published, database writes, external API calls are documented
- [ ] **Testability:** A junior developer can understand how to test this code from the Javadoc
- [ ] **Cross-references:** Use `{@link}` and `@see` tags to related classes, DTOs, events
- [ ] **Javadoc HTML:** Renders correctly with no broken links, proper formatting (`<p>`, `<ul>`, `<strong>`)
- [ ] **Quality Check:** Can a junior dev use the API correctly WITHOUT reading the implementation?

**Testing Compliance:**
- [ ] Integration tests use Testcontainers PostgreSQL 16.8 (NOT H2/HSQLDB)
- [ ] `@ServiceConnection` used for Spring Boot 3.1+ auto-wiring
- [ ] Unit tests achieve 80%+ coverage on service layer
- [ ] No `@SpringBootTest` for unit tests (use `@ExtendWith(MockitoExtension.class)`)

**Spring Modulith Compliance:**
- [ ] Entities, repositories, mappers, service implementations in `.internal` package
- [ ] DTOs, service interfaces, events at module root (public API)
- [ ] No cross-module entity/repository imports
- [ ] ApplicationModulesTest passes (verifies module boundaries)

**Consequences of Non-Compliance:**
- ❌ **CI Build Failure:** PR cannot be merged
- ❌ **Code Review Rejection:** PR marked "Request Changes" with specific violations listed
- ❌ **Technical Debt Tracking:** Violations logged in ADR for remediation
- ⚠️ **Zero Tolerance:** No exceptions to mandatory requirements (escalate to architect if unclear)

---
