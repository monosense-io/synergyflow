# Appendix: Section 4 - Component Architecture - Data Access Layer

This document contains full code implementations for MyBatis-Plus data access layer patterns referenced in `architecture.md` Section 4.4.

## MyBatis Lambda Query Examples

For architectural pattern overview, see `architecture.md` Section 4.4 "Data Access Layer (MyBatis-Plus)".

### Lambda Query Pattern

```java
// 1. Simple equality query
List<Incident> activeIncidents = incidentService.lambdaQuery()
    .eq(Incident::getStatus, "OPEN")
    .list();

// 2. Multiple conditions (AND)
List<Incident> highPriorityOpen = incidentService.lambdaQuery()
    .eq(Incident::getStatus, "OPEN")
    .eq(Incident::getPriority, Priority.HIGH)
    .orderByDesc(Incident::getCreatedAt)
    .list();

// 3. Pagination
Page<Incident> page = incidentService.lambdaQuery()
    .eq(Incident::getStatus, "OPEN")
    .page(new Page<>(1, 20));  // page 1, size 20
```

---

## MyBatis-Plus Configuration (Full)

### infrastructure/MyBatisPlusConfig.java

```java
@Configuration
@MapperScan("io.monosense.synergyflow.*.domain")  // Scan all module mappers
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

---

## Usage Notes

**Key Features:**
- `@MapperScan`: Auto-discovers all module mappers (pattern: `io.monosense.synergyflow.*.domain`)
- `PaginationInnerInterceptor`: Adds LIMIT/OFFSET to queries automatically
- `OptimisticLockerInnerInterceptor`: Implements optimistic locking via version field
- `BlockAttackInnerInterceptor`: Prevents SQL injection attacks
- `MetaObjectHandler`: Auto-fills timestamps on insert/update

**Service Inheritance:**
All services inherit from `ServiceImpl<M, T>` which provides:
- `save()`, `saveBatch()`, `saveOrUpdate()`
- `getById()`, `list()`, `page()`
- `lambdaQuery()` for type-safe queries
- `lambdaUpdate()` for type-safe updates

---

## Integration with Spring Modulith

MyBatis services integrate with Spring Modulith event publishing:

```java
@Service
public class IncidentServiceImpl extends ServiceImpl<IncidentMapper, Incident> {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public Incident createIncident(CreateIncidentCommand command) {
        Incident incident = new Incident(command);
        this.save(incident);  // MyBatis-Plus inherited method

        // Spring Modulith event published atomically
        eventPublisher.publishEvent(new IncidentCreatedEvent(incident));
        return incident;
    }
}
```

**Transactional Guarantee:** Both database insert and event publication occur in same transaction, ensuring atomicity.

---

## See Also

- **architecture.md Section 4.4:** Data Access Layer architectural pattern
- **architecture.md Section 5.2:** CQRS integration with event publishing
- **MyBatis-Plus Documentation:** https://baomidou.com/pages/24112f/
