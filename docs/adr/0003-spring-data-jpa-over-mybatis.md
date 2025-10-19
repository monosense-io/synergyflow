# ADR-0003: Spring Data JPA over MyBatis

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Backend Team

---

## Context

SynergyFlow backend requires a data access layer for PostgreSQL with:
- **CRUD operations** for 7 core entities (Incident, Change, Task, User, Worklog, DecisionReceipt, EventPublication)
- **Complex queries** with joins across tables (e.g., Incident → Related Changes → Worklogs)
- **Transactional integrity** (atomic entity + event publication writes)
- **Event-driven integration** with Spring Modulith
- **Rapid development** for 12-month delivery timeline with 3 backend developers

We needed to choose between:
1. **Spring Data JPA** (Hibernate provider) - declarative repository pattern
2. **MyBatis** - SQL-centric ORM with XML/annotation mappers
3. **JOOQ** - type-safe SQL query builder
4. **Raw JDBC** with SQL templates

Key constraints:
- Spring Boot 3.5.6 managed dependencies
- Small backend team (3 developers, not all expert in SQL optimization)
- Need for rapid feature delivery
- Integration with Spring Modulith events

---

## Decision

**We will use Spring Data JPA 3.2.0 with Hibernate 6.4.0 as the data access layer, NOT MyBatis.**

### Rationale

**1. Spring Boot Native Integration (Zero Configuration)**
- **Autoconfiguration**: Spring Boot manages Hibernate, connection pool, transaction manager automatically
- **Spring Modulith compatibility**: `@Transactional` integrates seamlessly with event publication
- **Consistent with ecosystem**: Same as other Spring Boot applications in platform

**2. Declarative Repository Pattern (10x Less Boilerplate)**
```java
// Spring Data JPA - No SQL required for CRUD
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByPriorityAndStatusOrderByCreatedAtDesc(
        Priority priority, IncidentStatus status);

    @Query("SELECT i FROM Incident i WHERE i.assignedTo = :userId AND i.status != 'CLOSED'")
    List<Incident> findActiveIncidentsByUser(@Param("userId") UUID userId);
}

// MyBatis equivalent - Requires XML mapper or annotations
@Mapper
public interface IncidentMapper {
    @Select("SELECT * FROM incidents WHERE status = #{status}")
    List<Incident> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM incidents WHERE priority = #{priority} AND status = #{status} ORDER BY created_at DESC")
    List<Incident> findByPriorityAndStatus(@Param("priority") String priority, @Param("status") String status);

    @Select("SELECT * FROM incidents WHERE assigned_to = #{userId} AND status != 'CLOSED'")
    List<Incident> findActiveIncidentsByUser(@Param("userId") String userId);
}
```

**3. Type-Safe Query Methods (Compile-Time Validation)**
- **Query derivation**: Method names generate SQL automatically
- **@Query with JPQL**: IDE autocomplete, refactoring support
- **Compilation fails**: If entity fields renamed, query methods break at compile time
- **MyBatis risk**: SQL strings in XML/annotations - runtime errors only

**4. Rapid Development (12-Month Delivery Timeline)**
- **CRUD operations**: Free with `JpaRepository` (no SQL writing)
- **Pagination/sorting**: Built-in with `PagingAndSortingRepository`
- **Auditing**: `@CreatedDate`, `@LastModifiedDate` annotations (automatic)
- **Specifications**: Type-safe dynamic queries for filtering

**5. Transaction Management Simplicity**
```java
@Service
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional  // Hibernate + Event Publication in one transaction
    public Incident createIncident(CreateIncidentRequest request) {
        Incident incident = incidentRepository.save(new Incident(...));
        eventPublisher.publishEvent(new IncidentCreatedEvent(incident.getId()));
        return incident;  // Event stored in event_publication table atomically
    }
}
```

**6. Developer Onboarding (Small Team)**
- **Standard pattern**: Most Spring Boot developers know Spring Data JPA
- **Less SQL expertise needed**: Junior developers can be productive immediately
- **IDE support**: IntelliJ IDEA Ultimate has excellent JPA tooling

---

## Consequences

### Positive

✅ **Declarative repository**: 10x less boilerplate vs MyBatis (no XML mappers)
✅ **Compile-time safety**: Method names validated, entity refactoring supported
✅ **Spring Boot integration**: Zero configuration, autoconfiguration works out-of-box
✅ **Rapid development**: CRUD, pagination, sorting, auditing built-in
✅ **Transaction management**: `@Transactional` integrates with Spring Modulith events
✅ **Developer productivity**: Junior developers productive immediately

### Negative

⚠️ **N+1 query risk**: Lazy loading can cause performance issues (mitigated: `@EntityGraph`, `JOIN FETCH`)
⚠️ **Less SQL control**: Complex queries harder to optimize (mitigated: `@Query` with native SQL if needed)
⚠️ **Hibernate learning curve**: Understanding lazy/eager loading, persistence context (mitigated: team training)
⚠️ **Generated SQL**: May not be optimal for all cases (mitigated: query profiling, native queries for hotspots)

### Trade-offs Accepted

We **accept**:
- Some complex queries may require native SQL (`@Query(nativeQuery=true)`)
- N+1 query risks (mitigated with fetch strategies, `@EntityGraph`)
- Hibernate-specific behaviors (lazy loading, dirty checking)

We **gain**:
- 10x faster development (no SQL boilerplate)
- Compile-time query validation
- Spring Boot native integration

---

## Alternatives Considered

### Alternative 1: MyBatis

**Approach**: SQL-centric ORM with XML mappers or annotation-based SQL.

**Rejected because**:
- **Boilerplate overhead**: Every query requires explicit SQL (CRUD, pagination, sorting)
- **No compile-time safety**: SQL in XML/annotations breaks at runtime, not compile time
- **Transaction complexity**: Manual integration with Spring Modulith event publication
- **Slower development**: 3 developers writing SQL for every operation
- **Less IDE support**: No autocomplete for SQL strings, no refactoring support

**When this makes sense**:
- Team with strong SQL expertise (DBA-level)
- Complex reporting queries requiring fine-tuned SQL
- Legacy database schema with non-standard conventions
- Performance-critical application where every query must be hand-optimized

---

### Alternative 2: JOOQ

**Approach**: Type-safe SQL query builder generating code from database schema.

**Rejected because**:
- **Code generation complexity**: Requires database schema to exist before code generation
- **Gradle plugin overhead**: Additional build step for JOOQ code generation
- **Less declarative**: More verbose than Spring Data JPA for simple queries
- **Learning curve**: Team must learn JOOQ DSL on top of SQL and Spring Boot
- **Overkill for CRUD**: JOOQ shines for complex queries, not simple CRUD

**When this makes sense**:
- Complex reporting application with many joins and aggregations
- Need for compile-time SQL validation without JPA abstractions
- Database-first development (schema exists before code)

---

### Alternative 3: Raw JDBC with Spring JdbcTemplate

**Approach**: Manual SQL with `JdbcTemplate` for query execution.

**Rejected because**:
- **Maximum boilerplate**: Manual row mapping, result set handling
- **No pagination/sorting**: Must implement manually for every query
- **No auditing**: Manual `created_at`, `updated_at` tracking
- **Error-prone**: SQL typos, column name mismatches only caught at runtime
- **Slow development**: 10x slower than Spring Data JPA

**When this makes sense**:
- Very simple application (1-2 tables)
- Batch processing with custom performance tuning
- Legacy system with existing JDBC code

---

## Validation

**How we validate this decision**:

1. **Query performance monitoring** (Continuous):
   - Monitor Hibernate SQL logs in development (show_sql=true)
   - Identify N+1 queries with Hibernate statistics
   - Alert if query count >10 per API request

2. **Performance benchmarking** (Month 3):
   - Target: Complex query latency p95 <50ms
   - Load test: 1,000 concurrent users
   - Success criteria: No queries >1 second

3. **Developer productivity** (Sprint retrospectives):
   - Track story velocity (story points per sprint)
   - Compare against MyBatis benchmark (if available)
   - Success criteria: 80% code coverage, <5 bugs per sprint related to data access

---

## When to Revisit This Decision

We should **reconsider MyBatis or native SQL** if:

1. **Query performance** becomes bottleneck (p95 >100ms for complex queries)
2. **N+1 queries** cannot be resolved with `@EntityGraph` or fetch strategies
3. **Complex reporting** requires hand-optimized SQL (window functions, CTEs, etc.)
4. **Team expertise** shifts to SQL-focused developers

We have a **fallback option**:
- Spring Data JPA supports `@Query(nativeQuery=true)` for complex queries
- Can use native SQL for hotspots while keeping JPA for CRUD
- Gradual migration to MyBatis possible if needed (repository interfaces remain)

---

## References

- **Spring Data JPA Documentation**: https://spring.io/projects/spring-data-jpa
- **Architecture Document**: [docs/architecture/11-backend-architecture.md](../architecture/11-backend-architecture.md)
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md) (Spring Data JPA 3.2.0, Hibernate 6.4.0)
- **PRD Infrastructure**: docs/PRD.md (lines 74-79 - Data Access Layer)
