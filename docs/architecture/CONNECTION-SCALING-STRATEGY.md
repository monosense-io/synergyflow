# Database Connection Scaling Strategy

**Date**: 2025-10-17
**Purpose**: Guide for scaling database connections from 250 to 1,000+ users
**Audience**: DevOps, Architecture, Backend Teams
**Status**: Reference Document for MVP → Phase 2 Transition

---

## Executive Summary

SynergyFlow uses a shared PostgreSQL cluster with PgBouncer connection pooling. The current configuration supports **250+ concurrent users** comfortably. This document outlines three scaling strategies for growing to **1,000+ users** without hitting connection limits.

| Scale | Backend Replicas | HikariCP/Pod | Total Connections | Cluster Limit | Status |
|-------|------------------|-------------|------------------|---------------|--------|
| **MVP** | 3 | 20 | 60 | 200 | ✅ Safe |
| **Phase 1** | 5-6 | 20 | 100-120 | 200 | ✅ Safe |
| **Phase 2 (Option A)** | 10 | 15-18 | 150-180 | 200 | ✅ Safe |
| **Phase 2 (Option B)** | 10 | 20 | 200 | 300+ | ⚠️ Requires Change |
| **Phase 2 (Option C)** | 10+ | 20 | 200+ | ∞ | ✅ Best Scale |

---

## Part 1: Current Configuration (MVP - 250 Users)

### Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│        Backend Deployment (3 replicas)               │
├──────────────────────────────────────────────────────┤
│                                                      │
│ Pod 1: HikariCP (10-20 connections)                 │
│ Pod 2: HikariCP (10-20 connections)                 │
│ Pod 3: HikariCP (10-20 connections)                 │
│                                                      │
│ Total: 30-60 connections                            │
└────────────┬─────────────────────────────────────────┘
             │ Spring Boot DataSource
             ▼
┌──────────────────────────────────────────────────────┐
│     PgBouncer Pooler (3 instances)                   │
├──────────────────────────────────────────────────────┤
│                                                      │
│ max_client_conn: 1000    (max inbound clients)       │
│ default_pool_size: 50    (connections per pooler)   │
│ max_db_connections: 50   (max to backend DB)        │
│                                                      │
│ Total: ~50 backend connections (multiplexed)        │
└────────────┬─────────────────────────────────────────┘
             │ PostgreSQL Protocol
             ▼
┌──────────────────────────────────────────────────────┐
│  PostgreSQL Shared Cluster (cnpg-system)             │
├──────────────────────────────────────────────────────┤
│                                                      │
│ synergyflow role: connectionLimit = 50               │
│ Cluster max_connections: 200                         │
│                                                      │
│ Headroom: 150 connections available                  │
└──────────────────────────────────────────────────────┘
```

### Connection Pooling Math

**HikariCP Configuration** (per backend pod):
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      connection-timeout: 5000ms
```

**3 Replicas × 20 connections = 60 total connections**

**PgBouncer Configuration** (transaction mode):
```
default_pool_size: 50          # Connections per pooler instance
max_client_conn: 1000           # Max inbound client connections
max_db_connections: 50          # Max to PostgreSQL database
```

**Result**: 1000 client connections → 50 PostgreSQL connections (20:1 multiplexing)

**Cluster Capacity**:
```
PostgreSQL cluster limit: 200 connections
synergyflow role limit: 50 connections
Current usage: 60 (backend) + overhead = ~70 connections
Headroom: ~130 connections (65% of cluster capacity)
```

---

## Part 2: Scaling to 1,000 Users - Three Options

### Option A: Reduce Connections Per Pod ✅ RECOMMENDED

**Target**: Reduce HikariCP connections from 20 to 15-18 per pod

**Configuration**:
```yaml
# OPTION A: Reduced connections per pod
spring:
  datasource:
    hikari:
      minimum-idle: 8
      maximum-pool-size: 15        # REDUCED from 20
      connection-timeout: 5000ms
```

**Capacity Math**:
| Metric | Value |
|--------|-------|
| Backend replicas | 10 |
| HikariCP per pod | 15 |
| Total backend connections | 150 |
| Cluster role limit | 200 |
| Utilization | 75% |
| Headroom | 50 connections |

**Advantages**:
- ✅ Minimal code changes (just config)
- ✅ No infrastructure changes needed
- ✅ Stays within existing cluster limits
- ✅ Performance impact negligible (15-20 connections per pod is still healthy)

**Disadvantages**:
- ⚠️ Slightly higher connection contention (smaller pool = more wait times)
- ⚠️ Less fault tolerance (fewer reserved connections)

**Implementation**:
```bash
# Update ConfigMap
kubectl set env deployment/synergyflow-backend \
  SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=15 \
  -n synergyflow

# Rolling restart
kubectl rollout restart deployment/synergyflow-backend -n synergyflow
kubectl rollout status deployment/synergyflow-backend -n synergyflow

# Verify
kubectl logs deployment/synergyflow-backend | grep "HikariPool"
# Should show: HikariPool - Pool stats (total=15, active=X, idle=Y)
```

**Performance Impact**:
- Connection wait time: +5-10ms p95 (acceptable)
- Throughput: -2-3% (minimal)
- CPU: No significant change

**Estimated Timeline**: 1-2 hours (mostly testing)

---

### Option B: Increase Cluster Role Limit ⚠️ REQUIRES COORDINATION

**Target**: Increase `synergyflow_app` role connection limit from 50 to 300+

**Configuration**:
```sql
-- On shared-postgres cluster
ALTER ROLE synergyflow_app CONNECTION LIMIT 300;
-- Verify
SELECT * FROM pg_user WHERE usename = 'synergyflow_app';
```

**Capacity Math** (with 20 connections per pod):
| Metric | Value |
|--------|-------|
| Backend replicas | 10 |
| HikariCP per pod | 20 |
| Total backend connections | 200 |
| Cluster role limit | 300 |
| Utilization | 67% |
| Headroom | 100 connections |

**Advantages**:
- ✅ No application code changes
- ✅ Supports full 20 connections per pod (best fault tolerance)
- ✅ More headroom for spikes

**Disadvantages**:
- ⚠️ Requires PostgreSQL superuser to execute
- ⚠️ Affects shared infrastructure (needs platform coordination)
- ⚠️ Increases pressure on shared cluster
- ⚠️ May require cluster capacity increase

**Implementation**:
```bash
# 1. Coordinate with infrastructure team (modify shared cluster)
# 2. Update synergyflow_app role limit
# 3. Restart SynergyFlow backend pods
kubectl rollout restart deployment/synergyflow-backend -n synergyflow

# 4. Monitor cluster connections
kubectl logs -f deployment/synergyflow-backend | grep "HikariPool"
```

**Risk Assessment**:
- ⚠️ **MEDIUM RISK**: Requires coordination with other platform teams
- ⚠️ **REQUIRES APPROVAL**: Infrastructure team must approve cluster change

**Estimated Timeline**: 2-3 days (includes coordination + change management)

---

### Option C: Migrate to Kafka ✅ BEST LONG-TERM

**Target**: Externalize event system to Kafka for independent scaling

**Current State** (Spring Modulith in-process):
```
Spring Boot App (3-10 replicas)
  └── In-memory event bus
      └── ~50 DB connections to shared cluster
```

**After Migration** (Kafka external):
```
Spring Boot App (3-10+ replicas)
  └── Kafka event producer/consumer
      └── Independent scaling

Kafka Cluster (3-5 brokers)
  ├── Topic: incidents.created
  ├── Topic: changes.approved
  ├── Topic: tasks.completed
  └── ...
```

**Capacity Impact**:
- Each additional backend replica doesn't increase DB connections proportionally
- Event consumers can scale independently
- Database connections remain ~50-100 regardless of replica count
- Supports 1,000+ users easily

**Advantages**:
- ✅ Unlimited horizontal scaling
- ✅ Event streaming for external integrations
- ✅ Natural migration from Spring Modulith
- ✅ Event replay for analytics
- ✅ Better operational visibility

**Disadvantages**:
- ❌ **Requires significant refactoring** (40-60 hours engineering effort)
- ❌ Additional infrastructure (Kafka cluster)
- ❌ Increased operational complexity
- ❌ Not needed for 1,000 users (Options A or B sufficient)

**Implementation**:
```bash
# Phase 1: Deploy Kafka cluster
helm install kafka bitnami/kafka \
  --namespace messaging \
  --values kafka-values.yaml

# Phase 2: Enable Spring Modulith event externalization
# Update application.yml
spring:
  modulith:
    events:
      external: kafka
      kafka:
        broker: kafka-cluster.messaging.svc.cluster.local:9092

# Phase 3: Deploy Kafka consumers
# Create consumer groups for external systems

# Phase 4: Test event flow
# Verify events published to Kafka topics
kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic incidents.created \
  --from-beginning
```

**Timeline**: 8-12 weeks (6-8 of active development)

**Effort**: 40-60 engineering hours

**When to Use**: Phase 2 Month 8+ (only if 1,000+ users confirmed AND external event consumers needed)

---

## Part 3: Recommendation & Decision Matrix

### Recommended Path by Growth Scenario

| Scenario | Current | Phase 1 | Phase 2 | Why |
|----------|---------|---------|---------|-----|
| **Conservative** (250→500 users) | Keep as-is | Option A | Done | Minimal risk, simple |
| **Aggressive** (250→1,000 users) | Keep as-is | Option A | Option B | Phased approach |
| **High growth** (1,000→5,000 users) | Keep as-is | Option A | Option C | Kafka enables unlimited scale |

### Decision Flowchart

```
┌─ Current at 250 users (3 replicas)
│
├─ Target: 500 users?
│  └─ YES: No action needed (keep as-is)
│  └─ NO: Continue
│
├─ Target: 1,000 users?
│  └─ YES: Go to Phase 2
│  └─ NO: Continue
│
├─ Target: 1,000+ users?
│  └─ YES: Continue
│  └─ NO: Go to Phase 2
│
└─ Phase 2 Decision (1,000+ users)
   │
   ├─ Need external event consumers?
   │  ├─ YES → Option C (Kafka migration)
   │  └─ NO → Continue
   │
   ├─ Can coordinate with platform team?
   │  ├─ YES → Option B (increase cluster limit) OR Option A
   │  └─ NO → Option A (reduce connections)
   │
   └─ Recommended: Option A (safe, simple, proven)
```

---

## Part 4: Implementation Steps by Option

### Option A: Reduce Connections (Recommended)

**Step 1: Update Configuration** (Week 1)
```yaml
# ConfigMap change
spring:
  datasource:
    hikari:
      minimum-idle: 8
      maximum-pool-size: 15
```

**Step 2: Test in Dev** (Week 1)
```bash
# Deploy to dev environment
# Monitor metrics:
# - Connection pool utilization
# - Wait time for connections
# - Database latency
# - Error rates

# Acceptance criteria:
# - No connection timeout errors
# - p95 latency < 200ms
# - Throughput ≥ 95% of baseline
```

**Step 3: Canary Deployment** (Week 2)
```bash
# Deploy to 1 backend pod first
# Monitor for 24 hours

# If OK → deploy to remaining pods
kubectl set env deployment/synergyflow-backend \
  SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=15 \
  -n synergyflow

kubectl rollout restart deployment/synergyflow-backend \
  -n synergyflow
```

**Step 4: Monitor Production** (Ongoing)
```bash
# Create dashboard:
# - HikariCP pool statistics
# - Connection wait times
# - Database connection count
# - Query latency by module

# Alert if:
# - Connection timeout > 0.5%
# - Latency p95 > 250ms
# - Pool saturation > 80%
```

**Rollback Plan** (if needed):
```bash
# Revert to previous configuration
kubectl set env deployment/synergyflow-backend \
  SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=20 \
  -n synergyflow

kubectl rollout restart deployment/synergyflow-backend \
  -n synergyflow
```

---

### Option B: Increase Cluster Limit

**Step 1: Get Infrastructure Approval** (Week 1-2)
- File infrastructure change request
- Document impact analysis
- Get sign-off from platform team

**Step 2: Plan Database Change** (Week 2)
```sql
-- Backup before change
pg_basebackup -h shared-postgres-primary -D ./backup

-- Check current state
SELECT rolname, rolconnlimit FROM pg_roles
WHERE rolname LIKE 'synergyflow%';

-- Execute during maintenance window (off-hours)
ALTER ROLE synergyflow_app CONNECTION LIMIT 300;

-- Verify
SELECT rolname, rolconnlimit FROM pg_roles
WHERE rolname = 'synergyflow_app';
```

**Step 3: Restart SynergyFlow** (Week 2)
```bash
kubectl rollout restart deployment/synergyflow-backend -n synergyflow
kubectl rollout status deployment/synergyflow-backend -n synergyflow
```

**Step 4: Monitor** (Ongoing)
```bash
# Verify cluster isn't overloaded
SELECT usename, count(*) as connection_count
FROM pg_stat_activity
GROUP BY usename
ORDER BY connection_count DESC;

# Alert if cluster connections approach 80% of max
```

---

### Option C: Kafka Migration

**Timeline**: 8-12 weeks (Plan for Phase 2 Month 8+)

**High-level Steps**:
1. Design Kafka topic schema (Week 1)
2. Deploy Kafka cluster (Week 2-3)
3. Implement Spring Modulith event externalization (Week 3-6)
4. Deploy Kafka consumers (Week 6-8)
5. Test & monitor (Week 8-10)
6. Sunset Spring Modulith in-process events (Week 10-12)

---

## Part 5: Monitoring & Metrics

### Key Metrics to Track

#### Connection Pool Metrics
```
HikariCP Metrics (Spring Boot Actuator):
- hikaricp_connections{state="active"}
- hikaricp_connections{state="idle"}
- hikaricp_connections{state="pending"}
- hikaricp_connection_timeout_total
- hikaricp_connection_acquire_nanos{quantile="0.95"}
```

#### Database Connection Metrics
```
PostgreSQL Metrics:
- pg_stat_activity (total connections)
- pg_stat_activity (by role)
- pg_stat_database (transaction rates)
```

#### Application Performance Metrics
```
REST API Latency:
- api_latency_ms{quantile="0.95"}  # Should be <200ms

Database Query Latency:
- query_duration_ms{quantile="0.95"}  # Should be <100ms

Throughput:
- requests_per_second
- database_operations_per_second
```

### Monitoring Dashboard (Grafana)

Create dashboard showing:
1. **Connection Pool Status**
   - HikariCP active/idle/pending
   - PgBouncer connection distribution
   - PostgreSQL role connections

2. **Performance Trends**
   - API latency (p50, p95, p99)
   - Database latency by operation type
   - Throughput (ops/sec)

3. **Alerts**
   - Connection pool saturation > 80%
   - Connection timeout errors
   - Query latency p95 > 200ms
   - Database connections > role limit

### Query to Monitor Connection Usage

```sql
-- Current connection state
SELECT
  usename,
  application_name,
  state,
  COUNT(*) as connection_count,
  STRING_AGG(query, '; ' ORDER BY backend_start DESC LIMIT 1) as latest_query
FROM pg_stat_activity
WHERE usename IN ('synergyflow_app', 'gitlab_app', 'harbor_app')
GROUP BY usename, application_name, state
ORDER BY connection_count DESC;

-- Peak connections by hour (from pg_stat_statements if available)
SELECT
  DATE_TRUNC('hour', NOW()) as hour,
  COUNT(DISTINCT backend_pid) as connections_in_use,
  MAX(pg_total_relation_size(schemaname||'.'||tablename)) as table_size
FROM pg_stat_activity
WHERE usename = 'synergyflow_app'
GROUP BY hour
ORDER BY hour DESC;
```

---

## Part 6: Decision Timeline

### MVP (Now - Week 4)
- ✅ Current configuration: 3 replicas, 20 HikariCP connections
- ✅ Supports: 250+ concurrent users
- ✅ Action: Monitor baseline metrics

### Phase 1 (Month 3-6)
- 📋 **Decision Point**: Are we at 500+ users?
- YES → Implement Option A (reduce to 15 connections)
- NO → Keep as-is

### Phase 2 (Month 6-12)
- 📋 **Decision Point**: Are we at 1,000+ users?
- YES → Implement Option A or B
- NO → Keep as-is

### Phase 3 (Month 12+)
- 📋 **Decision Point**: Do we need 5,000+ users or external integrations?
- YES → Plan Kafka migration (Option C)
- NO → Keep current scaling strategy

---

## Part 7: Recommendations

### For MVP (Now)
1. ✅ **Keep current configuration** (3 replicas, 20 HikariCP connections)
2. ✅ **Monitor baseline metrics** (connection pool, latency, throughput)
3. ✅ **Set up alerts** (connection pool saturation, latency thresholds)

### For Phase 1 (If scaling to 500+ users)
1. 📋 **Option A** (Recommended): Reduce HikariCP connections to 15
   - Effort: 1-2 hours
   - Risk: Low
   - Timeline: Week 1-2
   - Cost: $0

### For Phase 2 (If scaling to 1,000+ users)
1. 📋 **Option A** (Recommended): Already done in Phase 1
2. 📋 **Option B** (Fallback): Increase cluster role limit if Option A insufficient
   - Requires platform coordination
   - 2-3 day timeline

### For Phase 3 (If scaling to 5,000+ users or external integrations)
1. 📋 **Option C** (Required): Kafka migration
   - 8-12 week timeline
   - 40-60 engineering hours
   - Enables unlimited scaling

---

## Conclusion

**Current Recommendation**: Implement **Option A** when Phase 2 scaling becomes necessary (target: 1,000 users).

- ✅ Low risk (just configuration change)
- ✅ Minimal effort (1-2 hours)
- ✅ Proven approach (reduces connection footprint by 25%)
- ✅ No infrastructure changes
- ✅ Sufficient for 1,000 concurrent users

**Future Planning**:
- Monitor growth trajectory
- Revisit Option B/C if Option A shows limitations
- Plan Kafka migration for Phase 3+ if external integrations needed

---

**Document Status**: ✅ Complete - Ready for Phase 1/Phase 2 planning
**Last Updated**: 2025-10-17

