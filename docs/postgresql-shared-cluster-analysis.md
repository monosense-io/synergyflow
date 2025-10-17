# PostgreSQL Shared Cluster Architecture Analysis

**Date**: 2025-10-17
**Author**: Winston (Architect)
**Status**: Architecture Correction

---

## Executive Summary

**Issue Identified**: The current architecture specifies a **dedicated PostgreSQL cluster** for SynergyFlow, which is **inconsistent** with the platform's established pattern where all applications share a multi-tenant PostgreSQL cluster via PgBouncer poolers.

**Recommendation**: SynergyFlow should follow the platform pattern and use the **shared PostgreSQL cluster** (`shared-postgres` in `cnpg-system` namespace) with a dedicated PgBouncer pooler for connection management.

---

## 1. Current Platform Pattern

### 1.1 Shared PostgreSQL Cluster

**Location**: `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/platform/databases/cloudnative-pg/shared-cluster/cluster.yaml`

**Configuration**:
```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: shared-postgres
  namespace: cnpg-system
spec:
  description: "Shared multi-tenant PostgreSQL cluster for platform applications"
  imageName: ghcr.io/cloudnative-pg/postgresql:16.8
  instances: 3

  resources:
    requests:
      cpu: "2000m"
      memory: "8Gi"
    limits:
      cpu: "4000m"
      memory: "16Gi"

  storage:
    storageClass: ${CNPG_STORAGE_CLASS}
    size: ${CNPG_DATA_SIZE}

  # Managed Database Roles
  managed:
    roles:
      - name: gitlab_app
        ensure: present
        login: true
        connectionLimit: 50
        passwordSecret:
          name: gitlab-db-credentials

      - name: harbor_app
        ensure: present
        connectionLimit: 30
        passwordSecret:
          name: harbor-db-credentials

      - name: mattermost_app
        ensure: present
        connectionLimit: 40
        passwordSecret:
          name: mattermost-db-credentials

      - name: keycloak_app
        ensure: present
        connectionLimit: 20
        passwordSecret:
          name: keycloak-db-credentials
```

**Key Characteristics**:
- **Multi-tenant**: Designed to host multiple application databases
- **Larger resources**: 2-4 CPU, 8-16Gi memory (amortized across applications)
- **Managed roles**: Each application gets a dedicated PostgreSQL role with connection limits
- **Centralized operations**: Single cluster to backup, monitor, and maintain

### 1.2 PgBouncer Pooler Pattern

**Example**: GitLab Pooler (`gitlab-pooler.yaml`)

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Pooler
metadata:
  name: gitlab-pooler
  namespace: cnpg-system
spec:
  cluster:
    name: shared-postgres

  instances: 3
  type: rw

  pgbouncer:
    poolMode: transaction
    parameters:
      max_client_conn: "1000"
      default_pool_size: "50"
      min_pool_size: "10"
      max_db_connections: "50"

      server_idle_timeout: "600"
      server_lifetime: "3600"
      query_timeout: "300"

    authQuerySecret:
      name: gitlab-pooler-auth
    authQuery: "SELECT usename, passwd FROM user_search($1)"

  resources:
    requests:
      cpu: "500m"
      memory: "256Mi"
    limits:
      cpu: "2000m"
      memory: "512Mi"
```

**Pool Mode Selection**:
- **Transaction mode**: Used by gitlab, harbor, mattermost (default, most efficient)
- **Session mode**: Used by keycloak (required for prepared statements)

**Key Benefits**:
- **Connection pooling**: Reduces database connection overhead
- **Resource efficiency**: Multiplexes client connections to fewer database connections
- **High availability**: Multiple pooler instances with load balancing
- **Isolation**: Each application has its own pooler configuration

---

## 2. SynergyFlow Current Architecture (INCORRECT)

### 2.1 Dedicated Cluster Specification

**Location**: `architecture.md` lines 1384-1428, `k8s-gitops/kubernetes/workloads/apps/synergyflow/postgres-cluster.yaml`

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: synergyflow-db
  namespace: synergyflow
spec:
  instances: 3
  imageName: ghcr.io/cloudnative-pg/postgresql:16.6

  resources:
    requests:
      cpu: 1000m
      memory: 2Gi
    limits:
      cpu: 2000m
      memory: 2Gi

  storage:
    storageClass: rook-ceph-block
    size: 50Gi
```

**Problems with This Approach**:

1. **Inconsistent with Platform Pattern**:
   - All other applications (gitlab, harbor, keycloak, mattermost) use shared cluster
   - SynergyFlow would be the ONLY application with a dedicated cluster

2. **Resource Inefficiency**:
   - Dedicated 3-instance PostgreSQL cluster just for SynergyFlow
   - 3-6 CPU cores and 6Gi memory dedicated exclusively
   - Shared cluster resources underutilized

3. **Operational Overhead**:
   - Additional cluster to backup, monitor, upgrade
   - Separate backup strategy and retention policies
   - Duplicate operational complexity

4. **Namespace Confusion**:
   - Dedicated cluster in `synergyflow` namespace (apps cluster)
   - Shared cluster in `cnpg-system` namespace (infra cluster)
   - Violates multi-cluster architecture separation

5. **Flowable Compatibility**:
   - Flowable documentation explicitly states it can use shared databases
   - Research document (line 520-522): "Embedded engine (runs in your Spring Boot app) OR standalone server, Lower operational complexity than Temporal (no separate cluster)"
   - No technical requirement for dedicated cluster

---

## 3. Corrected Architecture (RECOMMENDED)

### 3.1 Shared Cluster with Pooler

**Step 1: Add Managed Role to Shared Cluster**

Edit `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/platform/databases/cloudnative-pg/shared-cluster/cluster.yaml`:

```yaml
managed:
  roles:
    # ... existing roles ...

    # SynergyFlow Application User
    - name: synergyflow_app
      ensure: present
      login: true
      superuser: false
      createdb: false
      createrole: false
      inherit: true
      replication: false
      connectionLimit: 50
      passwordSecret:
        name: synergyflow-db-credentials
```

**Step 2: Create PgBouncer Pooler**

Create `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/platform/databases/cloudnative-pg/poolers/synergyflow-pooler.yaml`:

```yaml
---
apiVersion: postgresql.cnpg.io/v1
kind: Pooler
metadata:
  name: synergyflow-pooler
  namespace: cnpg-system
spec:
  cluster:
    name: shared-postgres

  instances: 3
  type: rw

  pgbouncer:
    # Transaction mode suitable for Spring Boot + Flowable
    poolMode: transaction

    parameters:
      # Connection pool sizing (3 backend replicas × 20 HikariCP connections = 60)
      max_client_conn: "1000"
      default_pool_size: "50"
      min_pool_size: "10"
      reserve_pool_size: "10"
      reserve_pool_timeout: "5"
      max_db_connections: "50"
      max_user_connections: "50"

      # Timeouts
      server_idle_timeout: "600"
      server_lifetime: "3600"
      server_connect_timeout: "15"
      query_timeout: "300"
      query_wait_timeout: "120"
      client_idle_timeout: "0"
      idle_transaction_timeout: "600"

      # Logging
      log_connections: "1"
      log_disconnections: "1"
      log_pooler_errors: "1"
      stats_period: "60"

      # Security
      ignore_startup_parameters: "extra_float_digits,options"

    authQuerySecret:
      name: synergyflow-pooler-auth
    authQuery: "SELECT usename, passwd FROM user_search($1)"

  deploymentStrategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1

  resources:
    requests:
      cpu: "500m"
      memory: "256Mi"
    limits:
      cpu: "2000m"
      memory: "512Mi"

  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchLabels:
                cnpg.io/poolerName: synergyflow-pooler
            topologyKey: kubernetes.io/hostname

  template:
    metadata:
      labels:
        app.kubernetes.io/name: synergyflow-pooler
        app.kubernetes.io/component: connection-pooler
    spec:
      containers:
        - name: pgbouncer
          securityContext:
            allowPrivilegeEscalation: false
            runAsNonRoot: true
            readOnlyRootFilesystem: true
            seccompProfile:
              type: RuntimeDefault
            capabilities:
              drop:
                - ALL

  monitoring:
    enablePodMonitor: true
---
apiVersion: external-secrets.io/v1
kind: ExternalSecret
metadata:
  name: synergyflow-pooler-auth
  namespace: cnpg-system
spec:
  refreshInterval: 1h
  secretStoreRef:
    kind: ClusterSecretStore
    name: ${EXTERNAL_SECRET_STORE}
  target:
    name: synergyflow-pooler-auth
    creationPolicy: Owner
  data:
    - secretKey: username
      remoteRef:
        key: kubernetes/infra/cloudnative-pg/synergyflow
        property: username
    - secretKey: password
      remoteRef:
        key: kubernetes/infra/cloudnative-pg/synergyflow
        property: password
```

**Step 3: Update SynergyFlow Backend Connection**

**Connection String Change**:
```yaml
# OLD (dedicated cluster in synergyflow namespace):
jdbc:postgresql://synergyflow-db-rw.synergyflow.svc.cluster.local:5432/synergyflow

# NEW (pooler in cnpg-system namespace):
jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow
```

**Application Configuration** (`configmap.yaml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow
    username: ${DB_USERNAME}  # from synergyflow-db-credentials secret
    password: ${DB_PASSWORD}
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      connection-timeout: 5000
```

**Init Container Update** (`deployment.yaml`):
```yaml
initContainers:
  - name: wait-for-db
    image: postgres:16-alpine
    command:
      - sh
      - -c
      - |
        until pg_isready -h synergyflow-pooler-rw.cnpg-system.svc.cluster.local -p 5432 -U synergyflow; do
          echo "Waiting for database pooler..."
          sleep 2
        done
```

**Step 4: Database Initialization**

The shared cluster will automatically create the `synergyflow` database via managed role. Application bootstrap will create module schemas:

```sql
-- Created by Flowable/Flyway on first application startup
CREATE SCHEMA IF NOT EXISTS synergyflow_users;
CREATE SCHEMA IF NOT EXISTS synergyflow_incidents;
CREATE SCHEMA IF NOT EXISTS synergyflow_changes;
CREATE SCHEMA IF NOT EXISTS synergyflow_knowledge;
CREATE SCHEMA IF NOT EXISTS synergyflow_tasks;
CREATE SCHEMA IF NOT EXISTS synergyflow_audit;
CREATE SCHEMA IF NOT EXISTS synergyflow_workflows;  -- Flowable

-- Grant permissions to synergyflow_app role
GRANT ALL PRIVILEGES ON SCHEMA synergyflow_users TO synergyflow_app;
GRANT ALL PRIVILEGES ON SCHEMA synergyflow_incidents TO synergyflow_app;
-- ... etc for all schemas
```

---

## 4. Architectural Alignment

### 4.1 Spring Modulith Schema Isolation

**UNCHANGED**: Module-per-schema isolation pattern is preserved.

Architecture.md (lines 792-804) remains valid:
```
PostgreSQL Cluster
├── synergyflow_users (Foundation)
├── synergyflow_incidents
├── synergyflow_changes
├── synergyflow_knowledge
├── synergyflow_tasks
├── synergyflow_audit
└── synergyflow_workflows (Flowable)
```

**Key Point**: These are **schemas within the synergyflow database**, not separate databases. The schema isolation enforces module boundaries regardless of whether using dedicated or shared cluster.

### 4.2 Flowable Compatibility

**Flowable Requirements**:
- PostgreSQL 12+ (✅ shared cluster is PostgreSQL 16.8)
- Separate schema for Flowable tables (✅ `synergyflow_workflows` schema)
- Standard JDBC connection (✅ pooler provides standard PostgreSQL protocol)
- No requirement for dedicated cluster (✅ embedded mode uses shared DB)

**Transaction Mode Compatibility**:
- Spring Boot + Flowable work perfectly with PgBouncer transaction mode
- Flowable async job executor handles durable timers independently
- No prepared statement caching needed (unlike Keycloak which requires session mode)

### 4.3 Multi-Cluster Architecture

**Correct Pattern**:
```
┌─────────────────────────────────────────────────────────────┐
│                      Infra Cluster                          │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐    │
│  │            cnpg-system namespace                    │    │
│  ├────────────────────────────────────────────────────┤    │
│  │  • shared-postgres cluster (3 instances)           │    │
│  │  • gitlab-pooler                                    │    │
│  │  • harbor-pooler                                    │    │
│  │  • keycloak-pooler                                  │    │
│  │  • mattermost-pooler                                │    │
│  │  • synergyflow-pooler ← NEW                        │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Cross-cluster service access
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Apps Cluster                          │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐    │
│  │            synergyflow namespace                    │    │
│  ├────────────────────────────────────────────────────┤    │
│  │  • synergyflow-backend (3 replicas)                │    │
│  │    - Connects to synergyflow-pooler.cnpg-system    │    │
│  │  • synergyflow-frontend (3 replicas)               │    │
│  │  • OPA sidecars                                     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │            messaging namespace                      │    │
│  ├────────────────────────────────────────────────────┤    │
│  │  • Kafka cluster (3 brokers)                        │    │
│  │  • Schema Registry (2 replicas)                     │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**Benefits**:
- **Separation of concerns**: Infra cluster manages shared databases, apps cluster runs applications
- **Cross-cluster service consumption**: Apps in apps cluster consume database services from infra cluster
- **Consistent pattern**: All apps follow same pattern (gitlab, harbor, keycloak, mattermost, synergyflow)

---

## 5. Resource Impact Analysis

### 5.1 Current (Dedicated Cluster)

**Resources Required**:
| Component | Instances | CPU (Request) | CPU (Limit) | Memory (Request) | Memory (Limit) |
|-----------|-----------|---------------|-------------|------------------|----------------|
| PostgreSQL | 3 | 1000m | 2000m | 2Gi | 2Gi |
| **Total** | **3** | **3 cores** | **6 cores** | **6Gi** | **6Gi** |

**Storage**: 50Gi × 3 = 150Gi

**Operational Cost**:
- Dedicated backup configuration
- Separate monitoring dashboards
- Independent upgrade lifecycle
- Additional operational runbooks

### 5.2 Recommended (Shared Cluster + Pooler)

**Incremental Resources**:
| Component | Instances | CPU (Request) | CPU (Limit) | Memory (Request) | Memory (Limit) |
|-----------|-----------|---------------|-------------|------------------|----------------|
| Pooler | 3 | 500m | 2000m | 256Mi | 512Mi |
| **Total** | **3** | **1.5 cores** | **6 cores** | **768Mi** | **1.5Gi** |

**Storage**: 0Gi (shared cluster absorbs synergyflow database)

**Operational Benefit**:
- Shared backup infrastructure (no additional configuration)
- Existing monitoring dashboards (add synergyflow metrics)
- Coordinated upgrade with other platform apps
- Pooler follows standard platform pattern (reuse runbooks)

### 5.3 Savings

**Resource Savings**:
- CPU: 1.5 cores vs 3 cores (50% reduction)
- Memory: 768Mi vs 6Gi (87% reduction)
- Storage: 0Gi vs 150Gi (100% reduction)

**Operational Savings**:
- No dedicated PostgreSQL cluster to maintain
- Leverage shared infrastructure and operational procedures
- Consistent with platform's "boring technology" principle

---

## 6. Migration Path (If Dedicated Cluster Already Deployed)

### 6.1 Blue-Green Migration Strategy

**Prerequisites**:
1. Shared cluster has capacity for synergyflow database
2. PgBouncer pooler deployed and tested
3. Backup of dedicated cluster completed

**Migration Steps**:

**Step 1: Deploy Pooler** (No downtime)
```bash
kubectl apply -f synergyflow-pooler.yaml
kubectl wait --for=condition=Ready pooler/synergyflow-pooler -n cnpg-system
```

**Step 2: Database Export** (Read-only mode)
```bash
# Set dedicated cluster to read-only
kubectl exec -n synergyflow synergyflow-db-1 -- \
  psql -U synergyflow -c "ALTER DATABASE synergyflow SET default_transaction_read_only = on;"

# Dump database
kubectl exec -n synergyflow synergyflow-db-1 -- \
  pg_dump -U synergyflow synergyflow > synergyflow-backup.sql
```

**Step 3: Import to Shared Cluster**
```bash
# Create database in shared cluster
kubectl exec -n cnpg-system shared-postgres-1 -- \
  psql -U postgres -c "CREATE DATABASE synergyflow OWNER synergyflow_app;"

# Import dump
kubectl exec -i -n cnpg-system shared-postgres-1 -- \
  psql -U synergyflow_app synergyflow < synergyflow-backup.sql
```

**Step 4: Update Backend Configuration**
```bash
# Update connection string in ConfigMap
kubectl edit configmap synergyflow-config -n synergyflow
# Change: jdbc:postgresql://synergyflow-db-rw.synergyflow...
# To:     jdbc:postgresql://synergyflow-pooler-rw.cnpg-system...

# Rolling restart backend
kubectl rollout restart deployment/synergyflow-backend -n synergyflow
kubectl rollout status deployment/synergyflow-backend -n synergyflow
```

**Step 5: Validation**
```bash
# Verify backend connectivity
kubectl logs -n synergyflow deployment/synergyflow-backend | grep "Database connection"

# Run smoke tests
curl https://app.synergyflow.io/api/v1/health
```

**Step 6: Decommission Dedicated Cluster** (After 7-day grace period)
```bash
# Delete dedicated cluster
kubectl delete cluster synergyflow-db -n synergyflow

# Verify PVCs deleted
kubectl get pvc -n synergyflow
```

### 6.2 Rollback Plan

If issues detected within 7-day grace period:

```bash
# Revert ConfigMap
kubectl edit configmap synergyflow-config -n synergyflow
# Change back to: jdbc:postgresql://synergyflow-db-rw.synergyflow...

# Rolling restart
kubectl rollout restart deployment/synergyflow-backend -n synergyflow
```

---

## 7. Architecture Document Corrections

### 7.1 Section 10.3 Update

**Current (INCORRECT)** - Lines 1384-1428:
```yaml
**PostgreSQL Cluster** (CloudNative-PG):
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: synergyflow-db
  namespace: synergyflow
spec:
  instances: 3
  # ... dedicated cluster spec
```

**Corrected (RECOMMENDED)**:
```yaml
**PostgreSQL Database Access** (Shared Cluster + PgBouncer Pooler):

SynergyFlow uses the shared multi-tenant PostgreSQL cluster (`shared-postgres` in `cnpg-system` namespace) via a dedicated PgBouncer pooler for connection management.

**Architecture Pattern**:
- **Shared Cluster**: `shared-postgres` (PostgreSQL 16.8, 3 instances, 2-4 CPU, 8-16Gi)
- **Managed Role**: `synergyflow_app` with connection limit of 50
- **PgBouncer Pooler**: `synergyflow-pooler` (3 instances, transaction mode)
- **Database**: `synergyflow` with 7 schemas (users, incidents, changes, knowledge, tasks, audit, workflows)

**Pooler Configuration**:
apiVersion: postgresql.cnpg.io/v1
kind: Pooler
metadata:
  name: synergyflow-pooler
  namespace: cnpg-system
spec:
  cluster:
    name: shared-postgres
  instances: 3
  type: rw

  pgbouncer:
    poolMode: transaction
    parameters:
      max_client_conn: "1000"
      default_pool_size: "50"
      max_db_connections: "50"
      server_idle_timeout: "600"
      query_timeout: "300"

  resources:
    requests:
      cpu: 500m
      memory: 256Mi
    limits:
      cpu: 2000m
      memory: 512Mi

**Connection String**:
jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow

**Monitoring**:
- Pooler metrics via PodMonitor on port 9127
- PostgreSQL metrics from shared-postgres cluster
- Key pooler metrics:
  - `pgbouncer_stats_queries_total` - Query throughput
  - `pgbouncer_stats_client_connections_waiting` - Connection queue depth
  - `pgbouncer_pools_server_connections` - Backend connection usage
```

### 7.2 Section 3.3 Update

**Line 295-304** (Container Responsibilities - PostgreSQL):

**Current**:
```
**PostgreSQL Cluster (CloudNative-PG)**
- Technology: PostgreSQL 16.6 with CloudNative-PG operator
- Responsibilities:
  - Primary data store for all modules
  - Streaming replication (primary + 2 replicas)
  - Automatic failover on primary failure
  - Continuous WAL archiving for PITR
- Instances: 3 (1 primary, 2 replicas)
- Storage: 50GB per instance (rook-ceph-block)
- Resource: 1000m-2000m CPU, 2GB RAM per instance
```

**Corrected**:
```
**PostgreSQL Database Access (Shared Cluster + Pooler)**
- Technology: Shared PostgreSQL 16.8 cluster with CloudNative-PG operator + PgBouncer pooler
- Responsibilities:
  - Primary data store for all SynergyFlow modules
  - Connection pooling and multiplexing via PgBouncer
  - Schema-level isolation for module boundaries
  - Leverage shared cluster's HA and backup infrastructure
- Pooler Instances: 3 (high availability)
- Database: synergyflow within shared-postgres cluster
- Pooler Resource: 500m-2000m CPU, 256-512MB RAM per pooler instance
- Pattern: Consistent with other platform applications (gitlab, harbor, keycloak, mattermost)
```

### 7.3 Section 10.1 Update

**Line 2026-2027** (Namespace Structure):

**Current**:
```
- `synergyflow`: SynergyFlow application (backend, frontend, dedicated PostgreSQL)
- `cnpg-system`: Shared PostgreSQL cluster for platform applications
```

**Corrected**:
```
- `synergyflow`: SynergyFlow application (backend, frontend)
- `cnpg-system`: Shared PostgreSQL cluster for platform applications (including SynergyFlow)
```

### 7.4 Container Diagram Update

**Section 3.2** - Lines 211-217:

**Current**:
```
┌────────────────────────────────────────────────────────────┐
│              PostgreSQL Cluster (CloudNative-PG)           │
│  • 3 instances (primary + 2 replicas)                      │
│  • Streaming replication                                   │
│  • Automatic failover (<30s)                               │
│  • Schemas: users, incidents, changes, knowledge, tasks    │
└────────────────────────────────────────────────────────────┘
```

**Corrected**:
```
┌──────────────────────────────────────────────────────────────┐
│         Database Access (Shared Cluster Pattern)             │
│  ┌────────────────────────────────────────────────────┐     │
│  │  PgBouncer Pooler (cnpg-system namespace)          │     │
│  │  • 3 instances, transaction mode                   │     │
│  │  • Connection pooling (max 1000 clients → 50 DB)   │     │
│  └──────────────────┬─────────────────────────────────┘     │
│                     │ Cross-cluster service call            │
│                     ▼                                        │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Shared PostgreSQL (infra cluster)                 │     │
│  │  • 3 instances, PostgreSQL 16.8                    │     │
│  │  • Database: synergyflow                           │     │
│  │  • Schemas: users, incidents, changes, knowledge,  │     │
│  │    tasks, audit, workflows                          │     │
│  └────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────┘
```

---

## 8. Implementation Checklist

### Phase 1: Pooler Deployment (Week 1)

- [ ] Add `synergyflow_app` managed role to `shared-cluster/cluster.yaml`
- [ ] Create `synergyflow-pooler.yaml` in `poolers/` directory
- [ ] Create ExternalSecret for pooler authentication
- [ ] Apply pooler manifest to cluster
- [ ] Verify pooler pods are Ready
- [ ] Test connectivity: `pg_isready -h synergyflow-pooler-rw.cnpg-system.svc.cluster.local`

### Phase 2: Application Configuration (Week 2)

- [ ] Update `configmap.yaml` with new connection string
- [ ] Update init container in `deployment.yaml`
- [ ] Update ExternalSecret to reference correct Vault path
- [ ] Test backend startup in dev environment

### Phase 3: Migration (Week 3, if dedicated cluster exists)

- [ ] Set dedicated cluster to read-only mode
- [ ] Export database from dedicated cluster
- [ ] Import database to shared cluster
- [ ] Update backend configuration
- [ ] Rolling restart backend pods
- [ ] Run smoke tests
- [ ] Monitor for 7 days

### Phase 4: Documentation & Cleanup (Week 4)

- [ ] Update architecture.md Section 10.3
- [ ] Update architecture.md Section 3.3
- [ ] Update architecture.md Container Diagram
- [ ] Update infrastructure-deployment-manifests.md
- [ ] Update validation report with amendment
- [ ] Decommission dedicated cluster (if applicable)
- [ ] Update operational runbooks

---

## 9. Conclusion

**Recommendation**: SynergyFlow should adopt the shared PostgreSQL cluster + PgBouncer pooler pattern to:

1. **Align with Platform Standards**: Match the established pattern used by all other platform applications
2. **Reduce Resource Consumption**: 50% CPU savings, 87% memory savings, 100% storage savings
3. **Simplify Operations**: Leverage existing backup, monitoring, and upgrade procedures
4. **Maintain Module Isolation**: Schema-per-module pattern unchanged, works identically in shared cluster
5. **Support Multi-Cluster Architecture**: Proper separation between infra cluster (database services) and apps cluster (applications)

**Technical Compatibility**:
- Spring Boot: ✅ Works with PgBouncer transaction mode
- Flowable: ✅ No requirement for dedicated cluster (embedded mode)
- Schema Isolation: ✅ Enforced at database schema level, independent of cluster choice
- Connection Pooling: ✅ HikariCP (20 connections per pod) → PgBouncer (50 DB connections)

**Next Steps**:
1. Review this analysis with stakeholders
2. Create pooler manifest and test in dev environment
3. Update architecture.md with corrections
4. Plan migration timeline (if dedicated cluster already deployed)

---

**Document Status**: ✅ Complete - Ready for Architecture Review
