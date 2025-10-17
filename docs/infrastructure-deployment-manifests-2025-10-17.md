# SynergyFlow Infrastructure Deployment Manifests

**Date**: 2025-10-17
**Author**: Business Analyst Mary
**Target Cluster**: Apps Cluster (`/Users/monosense/iac/k8s-gitops`)

## Executive Summary

Production-ready Kubernetes manifests have been generated for the complete SynergyFlow ITSM+PM platform deployment, including:

1. **Kafka Event Backbone** (3-broker cluster with Strimzi)
2. **Schema Registry** (Confluent Community Edition)
3. **SynergyFlow Backend** (Spring Boot + Flowable + OPA sidecar)
4. **PostgreSQL Database** (CloudNative-PG with HA)
5. **Automation** (Taskfile with 30+ operations)
6. **GitOps Integration** (Flux Kustomizations)

All components are deployed to the **apps cluster** as requested.

## Generated Manifests

### 1. Kafka Event Backbone

**Location**: `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/platform/messaging/kafka/`

**Files**:
- `namespace.yaml` - Messaging namespace with pod security policies
- `helmrelease-strimzi.yaml` - Strimzi Kafka operator (v0.43.0)
- `kafka-cluster.yaml` - 3-broker Kafka cluster with KRaft mode
- `kafka-metrics-configmap.yaml` - JMX metrics configuration for Victoria Metrics
- `kafka-topics.yaml` - 11 pre-configured topics for SynergyFlow modules
- `kafka-users.yaml` - SCRAM-SHA-512 authenticated users with ACLs
- `kustomization.yaml` - Kustomize overlay

**Key Configuration**:
```yaml
Kafka Version: 3.8.1
Brokers: 3 (KRaft mode, no ZooKeeper)
Replication Factor: 3
Min In-Sync Replicas: 2
Storage: 100GB per broker (rook-ceph-block)
Resources: 2 CPU, 4GB RAM per broker
Authentication: SCRAM-SHA-512
Auto-create Topics: DISABLED (schema-first enforcement)
```

**Topics Created**:
| Topic | Partitions | Retention | Purpose |
|-------|-----------|-----------|---------|
| user-events | 6 | 7 days | User lifecycle |
| incident-events | 12 | 30 days | Incident management |
| change-events | 6 | 90 days | Change management |
| problem-events | 6 | 30 days | Problem management |
| workflow-events | 12 | 30 days | Flowable workflows |
| notification-events | 6 | 7 days | Notifications |
| cmdb-events | 6 | 90 days | CMDB changes |
| team-events | 3 | 30 days | Team/routing |
| knowledge-events | 3 | 30 days | Knowledge base |
| integration-events | 6 | 7 days | External integrations |
| dlq-events | 3 | 30 days | Dead letter queue |

### 2. Schema Registry

**Location**: `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/platform/messaging/schema-registry/`

**Files**:
- `deployment.yaml` - Confluent Schema Registry deployment (2 replicas)
- `service.yaml` - ClusterIP service (port 8081)
- `externalsecret.yaml` - Kafka credentials from Vault
- `servicemonitor.yaml` - Victoria Metrics integration
- `kustomization.yaml` - Kustomize overlay

**Key Configuration**:
```yaml
Version: Confluent Community Edition 7.8.0
Replicas: 2 (with leader election)
Compatibility: BACKWARD (default)
Kafka Topic: _schemas (RF=3)
Authentication: SCRAM-SHA-512 to Kafka
Resources: 250m CPU, 768MB RAM per replica
```

### 3. SynergyFlow Backend with Flowable + OPA

**Location**: `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/apps/synergyflow/`

**Files**:
- `namespace.yaml` - SynergyFlow namespace with pod security
- `postgres-cluster.yaml` - CloudNative-PG cluster (3 instances, HA)
- `externalsecrets.yaml` - DB, Kafka, Redis, S3 credentials from Vault
- `configmap.yaml` - Application configuration + OPA policy
- `deployment.yaml` - Spring Boot + OPA sidecar deployment (3 replicas)
- `service.yaml` - ClusterIP service with session affinity
- `servicemonitor.yaml` - Victoria Metrics integration
- `networkpolicy.yaml` - Network isolation and access control
- `kustomization.yaml` - Kustomize overlay
- `README.md` - Comprehensive deployment documentation

**Key Configuration**:

**PostgreSQL**:
```yaml
Version: PostgreSQL 16.6
Instances: 3 (automatic failover)
Storage: 50GB per instance (rook-ceph-block)
Backup: S3 with 30-day retention, AES256 encryption
Extensions: uuid-ossp, pg_stat_statements, pg_trgm
Schema: flowable (dedicated)
Resources: 1 CPU, 2GB RAM per instance
```

**Application**:
```yaml
Replicas: 3
Image: monosense/synergyflow-backend:latest
JVM: -Xms1024m -Xmx1024m, G1GC
Spring Boot: 3.x
Flowable: 7.x (embedded)
Resources: 500m-2000m CPU, 1.5-2GB RAM
Health Checks:
  - Liveness: /actuator/health/liveness
  - Readiness: /actuator/health/readiness
```

**OPA Sidecar**:
```yaml
Version: OPA 0.68.0
Policy: RBAC/ABAC with role hierarchy
Latency Target: <10ms (localhost)
Resources: 100m-500m CPU, 128-256MB RAM
API: http://localhost:8181/v1/data
```

**Init Containers**:
- `wait-for-db` - Ensures PostgreSQL is ready
- `wait-for-kafka` - Ensures Kafka is reachable

**Application Features**:
- Spring Boot 3.x with Spring Modulith architecture
- Flowable 7.x workflow engine (embedded)
- CloudEvents + Avro schema-first event publishing
- DragonflyDB caching integration
- Kafka producer/consumer with exactly-once semantics
- OPA policy enforcement for authorization
- Victoria Metrics observability

### 4. Automation (Taskfile)

**Location**: `/Users/monosense/iac/k8s-gitops/.taskfiles/synergyflow/Taskfile.yaml`

**Available Tasks**:

**Bootstrap**:
```bash
task synergyflow:bootstrap              # Bootstrap entire infrastructure
task synergyflow:bootstrap:messaging    # Bootstrap Kafka + Schema Registry
task synergyflow:bootstrap:app          # Bootstrap SynergyFlow backend
```

**Deployment**:
```bash
task synergyflow:deploy:kafka           # Deploy Kafka cluster
task synergyflow:deploy:schema-registry # Deploy Schema Registry
task synergyflow:deploy:app             # Deploy SynergyFlow backend
task synergyflow:deploy:flux-kustomizations # Deploy Flux automation
```

**Validation**:
```bash
task synergyflow:validate:all           # Validate entire deployment
task synergyflow:validate:kafka         # Validate Kafka health
task synergyflow:validate:schema-registry # Validate Schema Registry
task synergyflow:validate:app           # Validate SynergyFlow backend
task synergyflow:validate:connectivity  # Validate component connectivity
```

**Status & Monitoring**:
```bash
task synergyflow:status                 # Show all component status
task synergyflow:status:kafka           # Show Kafka status
task synergyflow:status:schema-registry # Show Schema Registry status
task synergyflow:status:app             # Show SynergyFlow status
task synergyflow:logs COMPONENT=app     # View component logs
```

**Database Management**:
```bash
task synergyflow:db:backup              # Trigger manual backup
task synergyflow:db:backups             # List available backups
task synergyflow:db:shell               # Open psql shell
```

**Kafka Management**:
```bash
task synergyflow:kafka:topics           # List all topics
task synergyflow:kafka:shell            # Open Kafka shell
```

**Development**:
```bash
task synergyflow:port-forward COMPONENT=app       # Forward backend port
task synergyflow:port-forward COMPONENT=kafka     # Forward Kafka port
task synergyflow:port-forward COMPONENT=postgres  # Forward PostgreSQL port
task synergyflow:restart COMPONENT=app            # Restart component
```

**Cleanup**:
```bash
task synergyflow:clean:all              # Remove all resources (DESTRUCTIVE)
task synergyflow:clean:app              # Remove SynergyFlow backend
task synergyflow:clean:messaging        # Remove messaging infrastructure
```

### 5. GitOps Integration (Flux)

**Location**: `/Users/monosense/iac/k8s-gitops/kubernetes/clusters/apps/`

**Files**:
- `messaging.yaml` - Flux Kustomizations for Kafka + Schema Registry
- `synergyflow.yaml` - Flux Kustomization for SynergyFlow backend

**Deployment Flow**:
```
cluster-apps-infrastructure
    │
    ├─► cluster-apps-messaging-kafka
    │       │
    │       └─► cluster-apps-messaging-schema-registry
    │                   │
    └───────────────────┴─► cluster-apps-synergyflow
```

**Health Checks**:
- Kafka: `kafka.strimzi.io/v1beta2` Ready condition
- Schema Registry: Deployment availability
- PostgreSQL: `postgresql.cnpg.io/v1` Cluster Ready condition
- SynergyFlow: Deployment availability

## Deployment Options

### Option 1: Manual Deployment

```bash
cd /Users/monosense/iac/k8s-gitops

# 1. Deploy Kafka
kubectl apply -k kubernetes/workloads/platform/messaging/kafka
kubectl wait --for=condition=Ready kafka/synergyflow -n messaging --timeout=600s

# 2. Deploy Schema Registry
kubectl apply -k kubernetes/workloads/platform/messaging/schema-registry
kubectl wait --for=condition=available deployment/schema-registry -n messaging --timeout=300s

# 3. Deploy SynergyFlow
kubectl apply -k kubernetes/workloads/apps/synergyflow
kubectl wait --for=condition=Ready cluster/synergyflow-db -n synergyflow --timeout=600s
kubectl wait --for=condition=available deployment/synergyflow-backend -n synergyflow --timeout=600s
```

### Option 2: Automated with Taskfile

```bash
cd /Users/monosense/iac/k8s-gitops

# One-command bootstrap
task synergyflow:bootstrap

# Validate deployment
task synergyflow:validate:all

# Check status
task synergyflow:status
```

### Option 3: GitOps with Flux

```bash
cd /Users/monosense/iac/k8s-gitops

# Apply Flux Kustomizations (one-time)
kubectl apply -f kubernetes/clusters/apps/messaging.yaml
kubectl apply -f kubernetes/clusters/apps/synergyflow.yaml

# Monitor reconciliation
flux get kustomizations -A
watch kubectl get kafka,kafkatopic,deployment,cluster -A
```

## Resource Requirements

### Minimum Cluster Resources

| Component | Replicas | CPU (Request) | CPU (Limit) | Memory (Request) | Memory (Limit) |
|-----------|---------|---------------|-------------|------------------|----------------|
| Kafka Brokers | 3 | 2 cores | 2 cores | 4 GiB | 4 GiB |
| Schema Registry | 2 | 250m | 1000m | 768 MiB | 1 GiB |
| PostgreSQL | 3 | 1000m | 2000m | 2 GiB | 2 GiB |
| SynergyFlow Backend | 3 | 500m | 2000m | 1.5 GiB | 2 GiB |
| OPA Sidecars | 3 | 100m | 500m | 128 MiB | 256 MiB |

**Total Requirements**:
- **CPU**: ~20 cores (requests), ~40 cores (limits)
- **Memory**: ~40 GiB (requests), ~50 GiB (limits)
- **Storage**: ~350 GiB (Kafka 300GB + PostgreSQL 150GB)

### Storage Classes

All persistent volumes use: `rook-ceph-block`

## Security Configuration

### Authentication

- **Kafka**: SCRAM-SHA-512 (username/password from Vault)
- **PostgreSQL**: User/password authentication (from Vault)
- **Schema Registry**: SCRAM-SHA-512 to Kafka
- **SynergyFlow**: OAuth2/JWT (via Envoy Gateway)

### Network Policies

SynergyFlow backend NetworkPolicy enforces:
- **Ingress**: Only from Envoy Gateway and Prometheus
- **Egress**: PostgreSQL, Kafka, Schema Registry, DragonflyDB, DNS, HTTPS

### Secrets Management

All secrets sourced from Vault via External Secrets Operator:
- `postgresql/synergyflow/*` - Database credentials
- `kafka/users/synergyflow-backend/*` - Kafka producer/consumer credentials
- `kafka/users/schema-registry/*` - Schema Registry credentials
- `redis/dragonfly/*` - Cache credentials
- `s3/backup/*` - S3 backup credentials

## Monitoring and Observability

### Metrics Collection

All components expose Prometheus metrics scraped by Victoria Metrics:

- **Kafka**: JMX metrics (port 9404) via `kafka-metrics-configmap.yaml`
- **Schema Registry**: `/metrics` endpoint (port 8081)
- **SynergyFlow Backend**: Spring Boot Actuator `/actuator/prometheus` (port 8080)
- **PostgreSQL**: CloudNative-PG metrics exporter

### Health Endpoints

- **SynergyFlow Backend**:
  - Liveness: `GET /actuator/health/liveness`
  - Readiness: `GET /actuator/health/readiness`
  - Metrics: `GET /actuator/prometheus`

- **Schema Registry**:
  - Health: `GET /subjects`

### Service Monitors

ServiceMonitor CRs created for automatic scraping:
- `synergyflow-backend` (namespace: synergyflow)
- `schema-registry` (namespace: messaging)
- Kafka metrics via Strimzi operator

## High Availability Configuration

### Kafka

- **Brokers**: 3 (KRaft mode)
- **Replication Factor**: 3
- **Min In-Sync Replicas**: 2
- **Rack Awareness**: `topology.kubernetes.io/zone`
- **Cruise Control**: Enabled for auto-rebalancing

### PostgreSQL

- **Instances**: 3 (primary + 2 replicas)
- **Automatic Failover**: CloudNative-PG operator
- **Backup**: S3 with 30-day retention
- **Point-in-Time Recovery**: Enabled

### Schema Registry

- **Replicas**: 2
- **Leader Election**: Enabled
- **Pod Anti-Affinity**: Preferred

### SynergyFlow Backend

- **Replicas**: 3
- **Pod Anti-Affinity**: Preferred (spread across nodes)
- **Session Affinity**: ClientIP (1-hour timeout)
- **Graceful Shutdown**: 60s termination grace period

## Backup and Recovery

### PostgreSQL Automatic Backups

Configured in `postgres-cluster.yaml`:
```yaml
backup:
  retentionPolicy: "30d"
  barmanObjectStore:
    destinationPath: "s3://synergyflow-backups/postgresql/"
    wal:
      compression: gzip
      encryption: AES256
    data:
      compression: gzip
      encryption: AES256
```

**Schedule**: Continuous WAL archiving + daily full backups
**Retention**: 30 days
**Storage**: S3-compatible (credentials from Vault)

### Manual Backup

```bash
task synergyflow:db:backup
# Creates backup: synergyflow-db-manual-YYYYMMDD-HHMMSS
```

## Troubleshooting

### Common Issues

**1. Kafka Not Starting**

```bash
# Check Strimzi operator
kubectl logs -n messaging -l name=strimzi-cluster-operator

# Verify storage class
kubectl get storageclass rook-ceph-block

# Check Kafka status
kubectl describe kafka synergyflow -n messaging
```

**2. Schema Registry Connection Issues**

```bash
# Verify Kafka user credentials
kubectl get secret schema-registry-kafka-credentials -n messaging -o yaml

# Check Schema Registry logs
kubectl logs -n messaging -l app.kubernetes.io/name=schema-registry

# Test connectivity
kubectl exec -n messaging <schema-registry-pod> -- \
  kafka-broker-api-versions --bootstrap-server synergyflow-kafka-bootstrap:9092
```

**3. SynergyFlow Backend Startup Failures**

```bash
# Check init containers
kubectl logs -n synergyflow <pod> -c wait-for-db
kubectl logs -n synergyflow <pod> -c wait-for-kafka

# Verify database
kubectl exec -n synergyflow synergyflow-db-1 -- pg_isready

# Check application logs
task synergyflow:logs COMPONENT=app

# Check OPA sidecar
task synergyflow:logs COMPONENT=opa
```

**4. External Secrets Not Syncing**

```bash
# Check External Secrets status
kubectl get externalsecrets -n synergyflow
kubectl get externalsecrets -n messaging

# Verify ClusterSecretStore
kubectl get clustersecretstore vault-backend

# Check External Secrets Operator logs
kubectl logs -n external-secrets-system -l app.kubernetes.io/name=external-secrets
```

## Next Steps

### Immediate Actions

1. **Validate Secrets**: Ensure all required secrets exist in Vault
2. **Deploy Infrastructure**: Run `task synergyflow:bootstrap`
3. **Verify Deployment**: Run `task synergyflow:validate:all`
4. **Configure Gateway**: Set up Envoy Gateway routes for SynergyFlow

### Application Development

1. **Build Backend Image**: Create Docker image for `monosense/synergyflow-backend:latest`
2. **Schema Registration**: Register Avro schemas in Schema Registry
3. **Flowable Workflows**: Deploy BPMN workflow definitions
4. **OPA Policies**: Refine authorization policies in `configmap.yaml`

### Monitoring Setup

1. **Dashboards**: Create Victoria Metrics/Grafana dashboards
2. **Alerts**: Configure alerting rules for SLOs
3. **Logging**: Set up log aggregation (if not already configured)

## References

- **Technical Research**: `/Users/monosense/repository/synergyflow/docs/research-technical-2025-10-17.md`
- **Brainstorming Results**: `/Users/monosense/repository/synergyflow/docs/brainstorming-session-results-2025-10-05.md`
- **Deployment README**: `/Users/monosense/iac/k8s-gitops/kubernetes/workloads/apps/synergyflow/README.md`
- **Flowable Docs**: https://www.flowable.com/open-source/docs
- **OPA Docs**: https://www.openpolicyagent.org/docs/
- **Strimzi Docs**: https://strimzi.io/docs/
- **CloudNative-PG Docs**: https://cloudnative-pg.io/documentation/

## Architecture Alignment

This deployment implements the recommendations from the technical research report:

✅ **Workflow Engine**: Flowable 7.x (embedded in Spring Boot)
✅ **Policy Engine**: OPA 0.68+ (sidecar pattern for <10ms latency)
✅ **Schema Registry**: Confluent Community Edition 7.8+
✅ **Event Pattern**: CloudEvents + Avro schema-first
✅ **Database**: PostgreSQL 16 with CloudNative-PG
✅ **Cache**: DragonflyDB (Redis-compatible)
✅ **Monitoring**: Victoria Metrics
✅ **GitOps**: Flux CD with Kustomize

### Key Differentiators

1. **Event-Driven Architecture**: Schema-first with Confluent Schema Registry
2. **Policy-First Authorization**: OPA sidecar with <10ms latency
3. **Workflow Orchestration**: Flowable 7.x for complex approval flows
4. **High Availability**: 3-replica deployments across all components
5. **GitOps Ready**: Flux Kustomizations with health checks
6. **Observable by Default**: ServiceMonitors for all components

## Conclusion

Production-ready Kubernetes manifests have been successfully generated for the complete SynergyFlow ITSM+PM platform. All components are configured for high availability, security, and observability, with comprehensive automation via Taskfile and GitOps integration via Flux.

**Total Deployment Time** (estimated):
- Manual: ~45 minutes
- Automated (Taskfile): ~30 minutes
- GitOps (Flux): ~35 minutes (initial sync)

**Recommended Deployment**: GitOps with Flux for production environments, Taskfile for development/testing.

---

**Generated**: 2025-10-17
**Analyst**: Business Analyst Mary
**Status**: ✅ Complete - Ready for Deployment
