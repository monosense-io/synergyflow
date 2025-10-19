# ADR-0002: Shared PostgreSQL Cluster Pattern

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Engineering Team, DevOps

---

## Context

SynergyFlow requires a production-grade PostgreSQL database with:
- **High availability** (99.9% uptime target)
- **Connection pooling** (support 1,000 max client connections → 50 DB connections)
- **Backup and recovery** (daily backups, point-in-time recovery)
- **Multi-namespace access** (SynergyFlow pods in `synergyflow` namespace access PostgreSQL in `cnpg-system` namespace)

Existing infrastructure already runs:
- **Shared PostgreSQL cluster** managed by CloudNativePG operator
- **PgBouncer pooler** for connection pooling (transaction mode)
- **Other applications** using same pattern: GitLab, Harbor, Keycloak, Mattermost

We needed to decide:
1. **Dedicated PostgreSQL cluster** for SynergyFlow (isolated infrastructure)
2. **Shared PostgreSQL cluster** following existing platform pattern
3. **Managed PostgreSQL** (AWS RDS, GCP Cloud SQL, Azure Database)

Key constraints:
- Self-hosted deployment (no cloud-managed databases)
- Consistent with existing platform infrastructure
- Cost efficiency (avoid duplicate infrastructure)
- Operational simplicity (single PostgreSQL cluster to manage)

---

## Decision

**We will use the shared PostgreSQL cluster pattern with PgBouncer pooler, NOT a dedicated cluster.**

### Architecture

```
SynergyFlow Backend (synergyflow namespace)
    ↓ JDBC connection
PgBouncer Pooler: synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432
    ↓ Connection pooling (1000 clients → 50 connections)
PostgreSQL Cluster (cnpg-system namespace)
    ↓ Database: synergyflow
    ├── Schema: synergyflow_incidents
    ├── Schema: synergyflow_changes
    ├── Schema: synergyflow_tasks
    ├── Schema: synergyflow_knowledge
    ├── Schema: synergyflow_users
    ├── Schema: synergyflow_audit
    └── Schema: synergyflow_workflows
```

### Connection Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow
    username: synergyflow_app
    hikari:
      maximum-pool-size: 20  # Per backend replica
      minimum-idle: 5
      connection-timeout: 30000
```

### Rationale

**1. Infrastructure Consistency (Critical for Operations)**
- **Same pattern as existing apps**: GitLab, Harbor, Keycloak, Mattermost use shared cluster
- **Single operational model**: One PostgreSQL cluster to monitor, backup, upgrade
- **Proven pattern**: Already battle-tested in production environment
- **DevOps familiarity**: Team knows this architecture, no learning curve

**2. Cost Efficiency (50% CPU, 87% Memory Savings)**
- **Avoid duplicate infrastructure**: No need for separate PostgreSQL cluster
- **Shared HA setup**: Reuse existing 3-node cluster with synchronous replication
- **Shared PgBouncer pooler**: Reuse connection pooling infrastructure
- **Estimated savings**:
  - CPU: 6 cores (3 PostgreSQL nodes × 2 cores each)
  - RAM: 20GB (3 nodes × 6-8GB each)
  - Cost: $100-150/month (cloud costs) or hardware costs (on-prem)

**3. High Availability Built-In**
- **CloudNativePG cluster**: 3-node cluster with automatic failover
- **Synchronous replication**: `min_in_sync_replicas=2` ensures zero data loss
- **PgBouncer HA**: 3 PgBouncer instances for pooler availability
- **Automatic failover**: <30 seconds downtime on primary failure

**4. Connection Pooling Optimization (20x Connection Reduction)**
- **Two-layer pooling**:
  1. **Application layer**: HikariCP with 20 connections per backend replica
  2. **Database layer**: PgBouncer transaction mode (1000 clients → 50 DB connections)
- **Benefits**:
  - Reduces PostgreSQL connection overhead (1000 → 50 connections)
  - Supports burst traffic (1000 concurrent clients)
  - Prevents connection exhaustion

**5. Isolation via Schema Separation**
- **Database-level isolation**: SynergyFlow has dedicated `synergyflow` database
- **Schema-level organization**: 7 schemas for module separation
- **No cross-application access**: Other apps (GitLab, Keycloak) in separate databases
- **Security**: Separate application user (`synergyflow_app`) with limited permissions

**6. Backup and Recovery Included**
- **Daily automated backups**: Managed by CloudNativePG operator
- **Point-in-time recovery (PITR)**: WAL archiving to S3-compatible storage
- **30-day retention**: Meets compliance requirements
- **No additional setup**: Already configured for shared cluster

---

## Consequences

### Positive

✅ **Infrastructure consistency**: Same pattern as existing applications (GitLab, Harbor, etc.)
✅ **Cost efficiency**: 50% CPU, 87% memory savings, $100-150/month avoided
✅ **High availability**: 3-node cluster with automatic failover, 99.9% uptime
✅ **Connection pooling**: 20x connection reduction (1000 clients → 50 DB connections)
✅ **Backup/recovery**: Daily backups, PITR, 30-day retention included
✅ **Operational simplicity**: Single PostgreSQL cluster to manage, monitor, upgrade

### Negative

⚠️ **Shared resource contention**: Heavy load from other apps could impact SynergyFlow (mitigated: resource limits, QoS)
⚠️ **Blast radius**: PostgreSQL cluster failure affects all applications (mitigated: HA cluster, automated failover)
⚠️ **Upgrade coordination**: PostgreSQL upgrades require coordination with other app teams (mitigated: maintenance windows)
⚠️ **Cross-namespace networking**: Requires CNI plugin support for cross-namespace communication (mitigated: standard Kubernetes feature)

### Trade-offs Accepted

We **accept**:
- Shared PostgreSQL cluster with other applications
- Cross-namespace communication dependency
- Potential resource contention (mitigated with limits)

We **gain**:
- 50% CPU, 87% memory savings
- Zero additional operational burden
- Proven HA architecture

---

## Alternatives Considered

### Alternative 1: Dedicated PostgreSQL Cluster for SynergyFlow

**Approach**: Deploy separate CloudNativePG cluster exclusively for SynergyFlow in `synergyflow` namespace.

**Rejected because**:
- **Duplicate infrastructure**: Requires 3 PostgreSQL nodes (6 CPU cores, 20GB RAM)
- **Operational overhead**: Separate cluster to monitor, backup, upgrade, scale
- **Cost inefficiency**: $100-150/month additional cloud costs or hardware
- **Inconsistent with platform**: Breaks existing infrastructure pattern
- **No technical benefit**: Same PostgreSQL features available in shared cluster

**When this makes sense**:
- Regulatory requirements mandate physical separation
- SynergyFlow scale exceeds shared cluster capacity (>10,000 concurrent users)
- Strict latency requirements (<5ms) not achievable with pooling
- Tenant isolation required (multi-tenant SaaS)

---

### Alternative 2: Managed PostgreSQL (AWS RDS, GCP Cloud SQL)

**Approach**: Use cloud provider's managed PostgreSQL service.

**Rejected because**:
- **Vendor lock-in**: Ties SynergyFlow to specific cloud provider
- **Self-hosted requirement**: Customers deploy on-premises or multi-cloud
- **Cost**: Managed PostgreSQL is 2-3x more expensive than self-hosted
- **Inconsistent with platform**: Other apps use self-hosted PostgreSQL
- **Data residency**: Cannot guarantee Indonesia data residency with all cloud providers

**When this makes sense**:
- SaaS multi-tenant deployment (not self-hosted)
- Cloud-only deployment strategy
- No in-house PostgreSQL expertise
- Need for managed backups, scaling, patching

---

### Alternative 3: Separate Database Per Backend Replica

**Approach**: Each backend replica connects to its own dedicated PostgreSQL database (partitioning).

**Rejected because**:
- **Data consistency complexity**: Cross-replica queries require distributed joins
- **Transaction boundaries**: Cannot span multiple databases (no cross-replica transactions)
- **Operational nightmare**: Multiple databases to backup, restore, migrate
- **No performance benefit**: PgBouncer pooling already handles connection load
- **Schema evolution hell**: Database migrations must run across all partitions

**When this makes sense**:
- Sharding strategy for massive scale (>100,000 users)
- Geographic distribution required (multi-region deployments)
- Read replicas for reporting workloads

---

## Validation

**How we validate this decision**:

1. **Connection pool monitoring** (Continuous):
   - Monitor HikariCP metrics: active connections, wait time, connection errors
   - Monitor PgBouncer metrics: client connections, server connections, wait queue
   - Alert if wait time >100ms or connection errors >1%

2. **Performance benchmarking** (Month 3):
   - Target: Database query latency p95 <50ms
   - Load test: 1,000 concurrent users
   - Success criteria: No connection timeouts, no pooler saturation

3. **High availability testing** (Month 6):
   - Simulate primary PostgreSQL failure
   - Measure failover time (target <30 seconds)
   - Verify zero data loss (synchronous replication)

4. **Resource contention monitoring** (Production):
   - Monitor PostgreSQL CPU/memory usage
   - Alert if SynergyFlow queries cause >50% CPU for >5 minutes
   - Implement query timeout (30 seconds) to prevent runaway queries

---

## When to Revisit This Decision

We should **reconsider dedicated PostgreSQL cluster** if:

1. **Resource contention** from other applications causes sustained SynergyFlow performance degradation
2. **Scale exceeds shared cluster capacity** (>10,000 concurrent users)
3. **Regulatory requirements** mandate physical database isolation
4. **Custom PostgreSQL extensions** needed that conflict with other applications

We have a **migration path**:
- CloudNativePG makes deploying new cluster trivial (same operator, same pattern)
- Database backup/restore enables migration to dedicated cluster
- Connection string change in SynergyFlow configuration (no code changes)

---

## References

- **CloudNativePG Documentation**: https://cloudnative-pg.io/
- **PgBouncer Documentation**: https://www.pgbouncer.org/
- **Architecture Document**: [docs/architecture/9-database-schema.md](../architecture/9-database-schema.md)
- **PRD Infrastructure**: docs/PRD.md (lines 68-73 - PostgreSQL cluster details)
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md) (PostgreSQL 16.8, PgBouncer 1.21+)
