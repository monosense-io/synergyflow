# Harbor Private Registry Deployment - ACTUAL ARCHITECTURE

**Date**: 2025-10-17
**Author**: Architectural Review (Revised)
**Target Cluster**: Infra Cluster (`/Users/monosense/iac/k8s-gitops`)
**GitOps Controller**: Flux CD (NOT Argo CD)

## 🚨 **EXECUTIVE SUMMARY - ARCHITECTURAL CORRECTIONS**

Harbor private container registry deployment with **accurate representation** of the actual infrastructure architecture:

1. **Shared PostgreSQL Architecture**: Multi-tenant CloudNative-PG cluster (NOT dedicated Harbor DB)
2. **1Password Connect Integration**: Secrets management via 1Password (NOT Vault)
3. **Flux CD GitOps**: Continuous deployment with Flux (NOT Argo CD)
4. **PgBouncer Connection Pooling**: Professional database connection management
5. **Centralized Backup Strategy**: MinIO-based backups for shared infrastructure

## 🏗️ **ACTUAL INFRASTRUCTURE ARCHITECTURE**

```
┌─────────────────────────────────────────────────────────────────┐
│                     Infra Cluster                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────────────────────────────────────────────┐     │
│  │              cnpg-system namespace                     │     │
│  │  ┌─────────────────────────────────────────────┐     │     │
│  │  │        shared-postgres (CNPG Cluster)       │     │     │
│  │  │  • 3 instances, PostgreSQL 16.8             │     │     │
│  │  │  • 500Gi data + 200Gi WAL storage          │     │     │
│  │  │  • Managed roles: harbor_app, gitlab_app    │     │     │
│  │  │  • PgBouncer poolers for each app          │     │     │
│  │  └─────────────────────────────────────────────┘     │     │
│  │                                                         │     │
│  │  ┌─────────────────────────────────────────────┐     │     │
│  │  │        harbor-pooler (PgBouncer)           │     │     │
│  │  │  • 3 instances, transaction pooling        │     │     │
│  │  │  • Port 6432, read-write access            │     │     │
│  │  │  • Connects harbor_app to shared-postgres  │     │     │
│  │  └─────────────────────────────────────────────┘     │     │
│  └───────────────────────────────────────────────────────┘     │
│                           │                                      │
│  ┌───────────────────────────────────────────────────────┐     │
│  │                harbor namespace                         │     │
│  │  ┌─────────────────────────────────────────────┐     │     │
│  │  │            Harbor (v1.16.0)                 │     │     │
│  │  │  Core Components (2 replicas each):        │     │     │
│  │  │  • Portal, Core, Registry, JobService      │     │     │
│  │  │  • Trivy Scanner (1 replica)               │     │     │
│  │  │  • Notary Server/Signer (1 replica each)  │     │     │
│  │  └─────────────────────────────────────────────┘     │     │
│  └───────────────────────────────────────────────────────┘     │
│                           │                                      │
│  ┌───────────────────────────────────────────────────────┐     │
│  │              1Password Connect                          │     │
│  │  • Host: http://opconnect.monosense.dev                │     │
│  │  • Stores: Prod (priority 1), Infra (priority 2)       │     │
│  │  • External Secrets Operator integration              │     │
│  └───────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                           │ HTTPS
                           ▼
                  harbor.monosense.io
                  (via Envoy Gateway)
```

## 📋 **FLUX CD GITOPS STRUCTURE**

### **Repository Structure**
```
k8s-gitops/
├── kubernetes/clusters/infra/
│   ├── flux-system/              # Flux CD bootstrap
│   │   └── gotk-sync.yaml       # GitRepository + Kustomization
│   ├── cluster-settings.yaml    # Cluster variables
│   ├── harbor.yaml              # Harbor Kustomization
│   ├── infrastructure.yaml      # Core infra dependencies
│   └── kustomization.yaml       # Cluster Kustomization
├── kubernetes/workloads/platform/
│   ├── registry/harbor/         # Harbor manifests
│   └── databases/cloudnative-pg/ # Shared PostgreSQL setup
│       ├── shared-cluster/      # Multi-tenant PostgreSQL
│       └── poolers/             # PgBouncer poolers
└── kubernetes/infrastructure/security/
    └── external-secrets/        # 1Password integration
```

### **Flux CD Deployment Flow**
```yaml
# 1. Flux CD bootstrap (gotk-sync.yaml)
GitRepository → cluster-config Kustomization

# 2. Infrastructure dependencies
infrastructure.yaml → External Secrets Operator → 1Password Connect

# 3. Harbor deployment
harbor.yaml → dependsOn: cluster-infra-infrastructure
               → healthChecks for PostgreSQL + ExternalSecrets
```

## 🔐 **1PASSWORD CONNECT SECRETS MANAGEMENT**

### **ClusterSecretStore Configuration**
```yaml
# kubernetes/infrastructure/security/external-secrets/clustersecretstore.yaml
apiVersion: external-secrets.io/v1
kind: ClusterSecretStore
metadata:
  name: ${EXTERNAL_SECRET_STORE}  # = "onepassword"
spec:
  provider:
    onepassword:
      connectHost: ${ONEPASSWORD_CONNECT_HOST}  # = "http://opconnect.monosense.dev"
      vaults:
        Prod: 1    # Primary production vault
        Infra: 2   # Infrastructure vault (priority 2)
```

### **Required 1Password Items (Infra Vault)**

| 1Password Item | Fields | Used By |
|----------------|--------|----------|
| `harbor-core` | `admin_password`, `secret_key` | Harbor HelmRelease values |
| `harbor-registry` | `http_secret` | Harbor registry HTTP secret |
| `kubernetes/infra/cloudnative-pg/harbor` | `username`, `password` | Harbor PostgreSQL role |
| `kubernetes/infra/dragonfly/auth` | `username`, `password` | DragonflyDB Redis |
| `kubernetes/infra/cloudnative-pg/minio` | `accessKey`, `secretKey` | PostgreSQL backups |

### **ExternalSecrets Configuration**
```yaml
# Harbor core secrets
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: harbor-core
  namespace: harbor
spec:
  secretStoreRef:
    name: ${EXTERNAL_SECRET_STORE}  # = "onepassword"
    kind: ClusterSecretStore
  data:
    - secretKey: HARBOR_ADMIN_PASSWORD
      remoteRef:
        key: harbor-core
        property: admin_password
    - secretKey: secret
      remoteRef:
        key: harbor-core
        property: secret_key
```

## 🗄️ **SHARED POSTGRESQL ARCHITECTURE**

### **Multi-tenant PostgreSQL Cluster**
```yaml
# shared-cluster/cluster.yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: shared-postgres
  namespace: cnpg-system
spec:
  instances: 3
  imageName: ghcr.io/cloudnative-pg/postgresql:16.8
  storage:
    size: ${CNPG_DATA_SIZE}    # 500Gi
  walStorage:
    size: ${CNPG_WAL_SIZE}     # 200Gi
  resources:
    requests: 8Gi memory, 2000m cpu
    limits: 16Gi memory, 4000m cpu
```

### **Harbor Managed Role**
```yaml
# Lines 226-238 in shared-cluster/cluster.yaml
managed:
  roles:
    - name: harbor_app
      ensure: present
      login: true
      superuser: false
      connectionLimit: 30
      passwordSecret:
        name: harbor-db-credentials
```

### **Harbor Connection Pooler**
```yaml
# poolers/harbor-pooler.yaml
apiVersion: postgresql.cnpg.io/v1
kind: Pooler
metadata:
  name: harbor-pooler
  namespace: cnpg-system
spec:
  cluster:
    name: shared-postgres
  instances: 3
  type: rw
  pgbouncer:
    poolMode: transaction
    max_client_conn: "500"
    default_pool_size: "25"
```

## 🔧 **CORRECTED HARBOR CONFIGURATION**

### **HelmRelease Database Configuration**
```yaml
# harbor/helmrelease.yaml - CORRECTED
database:
  type: external
  external:
    # ⭐ IMPORTANT: Connect to PgBouncer pooler, NOT direct PostgreSQL
    host: harbor-pooler-rw.cnpg-system.svc.cluster.local
    port: "6432"  # PgBouncer port, NOT 5432
    username: harbor_app  # Managed role, NOT "harbor"

    # Database names must exist in shared-postgres
    coreDatabase: harbor_registry        # NOT "registry"
    notaryServerDatabase: harbor_notary_server
    notarySignerDatabase: harbor_notary_signer

    sslmode: require
  # Password provided via ExternalSecret from 1Password
```

### **DragonflyDB Redis Configuration**
```yaml
# harbor/helmrelease.yaml - CORRECTED
redis:
  type: external
  external:
    addr: dragonfly.dragonfly.svc.cluster.local:6379
    # Separate databases for different Harbor components
    coreDatabaseIndex: "1"
    jobserviceDatabaseIndex: "2"
    registryDatabaseIndex: "3"
    trivyAdapterIndex: "4"
```

## 📋 **DEPLOYMENT PREREQUISITES**

### **1. 1Password Setup**
```bash
# Required 1Password items in "Infra" vault:
1. harbor-core
   - admin_password: <strong-password-min-8chars>
   - secret_key: <random-32-char-string>

2. harbor-registry
   - http_secret: <random-32-char-string>

3. kubernetes/infra/cloudnative-pg/harbor
   - username: harbor_app
   - password: <strong-password>

4. kubernetes/infra/dragonfly/auth
   - username: <dragonfly-username>
   - password: <dragonfly-password>

5. kubernetes/infra/cloudnative-pg/minio
   - accessKey: <minio-access-key>
   - secretKey: <minio-secret-key>
```

### **2. PostgreSQL Database Setup**
```sql
-- Connect to shared-postgres cluster as superuser
-- Create Harbor-specific databases
CREATE DATABASE harbor_registry;
CREATE DATABASE harbor_notary_server;
CREATE DATABASE harbor_notary_signer;

-- Grant permissions to harbor_app role
GRANT ALL PRIVILEGES ON DATABASE harbor_registry TO harbor_app;
GRANT ALL PRIVILEGES ON DATABASE harbor_notary_server TO harbor_app;
GRANT ALL PRIVILEGES ON DATABASE harbor_notary_signer TO harbor_app;

-- Install required extensions
\c harbor_registry
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### **3. Infrastructure Dependencies**
```bash
# Must be deployed before Harbor:
✅ 1Password Connect (http://opconnect.monosense.dev)
✅ External Secrets Operator
✅ CloudNative-PG Operator
✅ shared-postgres cluster
✅ harbor-pooler (PgBouncer)
✅ DragonflyDB Redis
✅ Rook-Ceph storage
```

## 🚀 **FLUX CD DEPLOYMENT PROCEDURE**

### **Option 1: Automated GitOps (Recommended)**
```bash
cd /Users/monosense/iac/k8s-gitops

# 1. Verify cluster configuration
cat kubernetes/clusters/infra/cluster-settings.yaml | grep EXTERNAL_SECRET_STORE
# Should output: EXTERNAL_SECRET_STORE: "onepassword"

# 2. Deploy infrastructure dependencies
flux reconcile kustomization cluster-infra-infrastructure -n flux-system

# 3. Deploy Harbor (Flux will wait for dependencies)
flux reconcile kustomization cluster-infra-harbor -n flux-system

# 4. Monitor deployment
flux get kustomizations -n flux-system | grep harbor
kubectl get helmrelease -n harbor -w
```

### **Option 2: Manual Kubectl**
```bash
cd /Users/monosense/iac/k8s-gitops

# 1. Apply Harbor Kustomization
kubectl apply -k kubernetes/workloads/platform/registry/harbor

# 2. Wait for ExternalSecrets to be ready
kubectl wait --for=condition=Ready externalsecret/harbor-core -n harbor --timeout=300s
kubectl wait --for=condition=Ready externalsecret/harbor-registry -n harbor --timeout=300s
kubectl wait --for=condition=Ready externalsecret/harbor-database -n harbor --timeout=300s

# 3. Monitor Harbor deployment
kubectl get pods -n harbor -w
```

## ✅ **DEPLOYMENT VERIFICATION**

### **1. Check All Resources**
```bash
# Harbor components
kubectl get all -n harbor

# PostgreSQL cluster
kubectl get cluster shared-postgres -n cnpg-system

# Harbor pooler
kubectl get pooler harbor-pooler -n cnpg-system

# ExternalSecrets
kubectl get externalsecrets -n harbor
kubectl get secrets -n harbor
```

### **2. Test Database Connectivity**
```bash
# Test Harbor pooler connection
kubectl run harbor-db-test --rm -it --image=postgres:16-alpine \
  --restart=Never -- \
  psql -h harbor-pooler-rw.cnpg-system.svc.cluster.local -p 6432 \
       -U harbor_app -d harbor_registry -c "SELECT version();"
```

### **3. Test Harbor API**
```bash
# Test Harbor API endpoint
kubectl run curl-test --rm -it --image=curlimages/curl:latest \
  --restart=Never -- \
  curl -f http://harbor-core.harbor.svc.cluster.local/api/v2.0/systeminfo
```

## 🔧 **POST-DEPLOYMENT CONFIGURATION**

### **1. Access Harbor UI**
**URL**: https://harbor.monosense.io
**Username**: `admin`
**Password**: Get from 1Password → Infra vault → `harbor-core` → `admin_password`

### **2. Create SynergyFlow Project**
1. Login to Harbor UI
2. Click **"New Project"**
3. Project Name: `synergyflow`
4. Access Level: Private
5. Click **"OK"**

### **3. Create Robot Account for SynergyFlow**
1. Navigate to **Projects → synergyflow**
2. Click **Robot Accounts** tab
3. Click **"New Robot Account"**
4. Configuration:
   - Name: `synergyflow-puller`
   - Expiration: Never
   - Permissions: ✓ Push, ✓ Pull
5. Click **"Add"**
6. **IMPORTANT**: Copy the generated token (displayed once)
7. Store in 1Password → Infra vault → `harbor-robot-synergyflow`

### **4. Update 1Password with Robot Account**
```bash
# Create new 1Password item: harbor-robot-synergyflow
# Fields:
# - username: robot$synergyflow-puller
# - password: <copied-token-from-harbor-ui>
```

### **5. Update SynergyFlow ExternalSecret**
```yaml
# kubernetes/workloads/apps/synergyflow/externalsecrets.yaml
# Already exists - just ensure 1Password item is created
data:
  - secretKey: username
    remoteRef:
      key: harbor-robot-synergyflow  # New 1Password item
      property: username
  - secretKey: password
    remoteRef:
      key: harbor-robot-synergyflow
      property: password
```

## 🔍 **MONITORING AND OBSERVABILITY**

### **Flux CD Monitoring**
```bash
# Check Flux reconciliation status
flux get kustomizations -n flux-system
flux get helmreleases -n harbor

# Check Harbor health checks
kubectl get kustomization cluster-infra-harbor -n flux-system -o yaml | grep -A 10 healthChecks
```

### **PostgreSQL Monitoring**
```bash
# Check shared-postgres cluster
kubectl get cluster shared-postgres -n cnpg-system
kubectl get pods -n cnpg-system -l cnpg.io/cluster=shared-postgres

# Check Harbor pooler
kubectl get pooler harbor-pooler -n cnpg-system
kubectl logs -n cnpg-system -l cnpg.io/poolerName=harbor-pooler
```

### **Harbor Metrics**
```bash
# Harbor ServiceMonitors are automatically created
kubectl get servicemonitors -n harbor

# Check metrics endpoints
kubectl port-forward -n harbor svc/harbor-core 8001:8001
curl http://localhost:8001/metrics
```

## 🚨 **TROUBLESHOOTING**

### **Harbor Fails to Start**
```bash
# Check ExternalSecrets
kubectl get externalsecrets -n harbor
kubectl describe externalsecret harbor-core -n harbor

# Check 1Password connectivity
kubectl logs -n external-secrets deployment/external-secrets-operator

# Verify 1Password items exist
# Use 1Password CLI or UI to verify all required items exist in Infra vault
```

### **Database Connection Issues**
```bash
# Check shared-postgres cluster
kubectl get cluster shared-postgres -n cnpg-system
kubectl describe cluster shared-postgres -n cnpg-system

# Check Harbor pooler
kubectl get pooler harbor-pooler -n cnpg-system
kubectl describe pooler harbor-pooler -n cnpg-system

# Test connectivity from Harbor pod
kubectl exec -n harbor <harbor-core-pod> -- \
  psql -h harbor-pooler-rw.cnpg-system.svc.cluster.local -p 6432 \
       -U harbor_app -d harbor_registry -c "SELECT 1;"
```

### **Flux CD Issues**
```bash
# Check Flux logs
flux logs -f

# Check reconciliation status
flux get kustomizations -n flux-system
flux reconcile kustomization cluster-infra-harbor -n flux-system

# Check HelmRelease status
kubectl get helmrelease harbor -n harbor
kubectl describe helmrelease harbor -n harbor
```

## 📋 **MAINTENANCE**

### **Regular Tasks**
**Weekly**:
- Review Harbor vulnerability scan results
- Check shared-postgres cluster health
- Review 1Password access logs

**Monthly**:
- Review Harbor project access and RBAC
- Check PostgreSQL pooler performance
- Test backup/restore procedures

**Quarterly**:
- Update Harbor to latest version
- Review 1Password vault access
- Update CloudNative-PG operator

### **Backup Strategy**
- **PostgreSQL**: Automatic via CloudNative-PG to MinIO (`s3://monosense-cnpg/shared-postgres`)
- **Harbor Registry**: Manual volume snapshots or Velero integration
- **1Password**: Managed by 1Password service

## 🎯 **KEY DIFFERENCES FROM ORIGINAL DOCUMENTATION**

| Aspect | ❌ Original Doc | ✅ Actual Setup |
|--------|----------------|-----------------|
| **GitOps** | Argo CD hooks | Flux CD healthChecks |
| **PostgreSQL** | Dedicated Harbor cluster | Shared multi-tenant cluster |
| **Connection** | Direct PostgreSQL | PgBouncer pooler |
| **Secrets** | Vault paths | 1Password items |
| **Namespace** | `harbor` database | `cnpg-system` shared cluster |
| **Backup** | S3 harbor-backups | MinIO monosense-cnpg |
| **Deployment** | Manual procedures | Flux CD GitOps |
| **Architecture** | Siloed Harbor setup | Multi-tenant shared infrastructure |

## 📊 **RESOURCE ALLOCATION**

### **Harbor Components**
| Component | Replicas | CPU (Request/Limit) | Memory (Request/Limit) |
|-----------|---------|---------------------|------------------------|
| Portal | 2 | 100m/500m | 256Mi/512Mi |
| Core | 2 | 250m/1000m | 512Mi/1Gi |
| Registry | 2 | 250m/1000m | 512Mi/1Gi |
| JobService | 2 | 250m/1000m | 512Mi/1Gi |
| Trivy | 1 | 200m/1000m | 512Mi/1Gi |
| Notary Server | 1 | 100m/500m | 256Mi/512Mi |
| Notary Signer | 1 | 100m/500m | 256Mi/512Mi |

### **Shared Infrastructure**
| Component | Harbor Share | Total Allocation |
|-----------|--------------|------------------|
| PostgreSQL | harbor_app role (30 conn limit) | 16Gi RAM, 4 vCPU |
| PgBouncer | harbor-pooler (3 instances) | 256Mi RAM, 1 vCPU |
| Storage | Registry: 200Gi, Images: ~300Gi total | Shared cluster storage |

## 🔗 **REFERENCES**

- **Flux CD Documentation**: https://fluxcd.io/flux/
- **CloudNative-PG**: https://cloudnative-pg.io/documentation/
- **Harbor Documentation**: https://goharbor.io/docs/
- **1Password Connect**: https://developer.1password.com/docs/connect/
- **PgBouncer**: https://pgbouncer.github.io/

---

**Status**: ✅ **Revised - Ready for Deployment**
**Architecture**: Multi-tenant shared infrastructure with professional PostgreSQL setup
**GitOps**: Flux CD continuous deployment
**Secrets**: 1Password Connect integration
**Total Deployment Time**: 15-20 minutes (including dependency resolution)