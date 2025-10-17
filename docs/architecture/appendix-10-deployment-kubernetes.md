# Appendix: Section 10 - Deployment Architecture - Kubernetes Manifests

This document contains full Kubernetes configurations, infrastructure setup, and GitOps patterns referenced in `architecture.md` Section 10.

For architectural pattern overview, see `architecture.md` Section 10 "Deployment Architecture".

---

## PostgreSQL HA Configuration (CloudNative-PG)

### K8s Pod Security Policy and Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secrets
  namespace: default
type: Opaque
data:
  POSTGRES_PASSWORD: {{ base64_encode(postgres_password) }}
  POSTGRES_REPLICATION_PASSWORD: {{ base64_encode(replication_password) }}

---
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: postgres-ha
  namespace: default
spec:
  instances: 3
  imageName: ghcr.io/cloudnative-pg/postgresql:16.8-1

  bootstrap:
    initdb:
      database: synergyflow
      owner: synergyflow
      secret:
        name: postgres-secrets

  postgresql:
    parameters:
      max_connections: "500"
      shared_buffers: "2GB"
      effective_cache_size: "6GB"
      maintenance_work_mem: "512MB"
      checkpoint_completion_target: "0.9"
      wal_buffers: "16MB"
      default_statistics_target: "100"
      random_page_cost: "1.1"
      effective_io_concurrency: "200"
      work_mem: "10MB"
      synchronous_commit: "on"
      min_wal_level: "replica"

  backup:
    barmanObjectStore:
      destinationPath: s3://synergyflow-backups/postgres
      s3Credentials:
        accessKeyId:
          name: backup-s3-secret
          key: ACCESS_KEY
        secretAccessKey:
          name: backup-s3-secret
          key: SECRET_KEY
      compression: gzip
      retentionPolicy: "30d"

  monitoring:
    podMonitorMetricsEndpoints:
      - port: metrics

  resources:
    requests:
      memory: "2Gi"
      cpu: "1000m"
    limits:
      memory: "4Gi"
      cpu: "2000m"
```

---

## JWT Validation Security Policy (Envoy Gateway)

### SecurityPolicy with JWT Validation

```yaml
apiVersion: gateway.networking.k8s.io/v1beta1
kind: SecurityPolicy
metadata:
  name: jwt-validation-policy
  namespace: default
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: protected-routes
  jwt:
    providers:
    - name: oauth2-provider
      issuer: "https://oauth2.example.com"
      audience: "synergyflow-api"
      jwksUri: "https://oauth2.example.com/.well-known/jwks.json"
    rules:
    - matches:
      - path:
          type: PathPrefix
          value: /api/v1
      provider:
        name: oauth2-provider
        audiences:
        - "synergyflow-api"
      claimToHeaders:
      - claim: sub
        header: X-User-ID
      - claim: email
        header: X-User-Email
      - claim: roles
        header: X-User-Roles
```

---

## Rate Limiting Configuration (Envoy Gateway)

### BackendTrafficPolicy with Rate Limiting

```yaml
apiVersion: gateway.networking.k8s.io/v1alpha2
kind: BackendTrafficPolicy
metadata:
  name: rate-limit-policy
  namespace: default
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: api-routes
  rateLimit:
    type: Global
    global:
      rules:
      - clientSelectors: []
        limit: 100
        window: 1m
        action:
          type: Custom
          customReply:
            statusCode: 429
            headers:
              Content-Type: "application/json"
            body: |
              {
                "error": "Rate limit exceeded",
                "limit": 100,
                "window": "1m",
                "retry_after": 60
              }
```

---

## Frontend Deployment with SSR

### Next.js Application Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: synergyflow-frontend
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: synergyflow-frontend
  template:
    metadata:
      labels:
        app: synergyflow-frontend
        version: v1
    spec:
      containers:
      - name: nextjs-app
        image: harbor.example.com/synergyflow/frontend:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 3000
          name: http
        env:
        - name: NEXT_PUBLIC_API_URL
          value: "https://api.synergyflow.local/api/v1"
        - name: NEXTAUTH_URL
          value: "https://synergyflow.local"
        - name: NEXTAUTH_SECRET
          valueFrom:
            secretKeyRef:
              name: nextauth-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /api/health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/ready
            port: 3000
          initialDelaySeconds: 10
          periodSeconds: 5
        terminationGracePeriodSeconds: 30
---
apiVersion: v1
kind: Service
metadata:
  name: synergyflow-frontend
  namespace: default
spec:
  selector:
    app: synergyflow-frontend
  ports:
  - port: 80
    targetPort: 3000
    name: http
  type: ClusterIP
```

---

## DragonflyDB Cache Configuration

### DragonflyDB Connection and Pooling

```yaml
# application.yml
spring:
  data:
    redis:
      host: dragonfly.dragonfly-system.svc.cluster.local
      port: 6379
      timeout: 2000ms
      jedis:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

cache:
  dragonfly:
    default-ttl: 3600
    max-entries: 100000
    eviction-policy: "allkeys-lru"
```

### Java Configuration

```java
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisCacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            );

        return RedisCacheManager.create(connectionFactory);
    }
}
```

---

## PgBouncer Connection Pooling

### PgBouncer Configuration (Full)

```ini
[databases]
synergyflow = host=postgres-ha port=5432 user=synergyflow password=XXXX

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 20
min_pool_size = 5
reserve_pool_size = 5
reserve_pool_timeout = 3

stats_users = stats
stats_password = stats_password

admin_users = admin
admin_password = admin_password

query_wait_timeout = 120
client_idle_timeout = 600
server_lifetime = 3600

log_connections = 1
log_disconnections = 1
log_pooler_errors = 1

listen_port = 6432
listen_addr = 0.0.0.0
unix_socket_dir = /var/run/pgbouncer
```

### PgBouncer K8s Sidecar

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: synergyflow-backend
spec:
  template:
    spec:
      containers:
      - name: app
        image: harbor.example.com/synergyflow/backend:latest
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://localhost:6432/synergyflow"
        - name: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
          value: "20"

      - name: pgbouncer
        image: pgbouncer:1.21.0
        ports:
        - containerPort: 6432
        volumeMounts:
        - name: pgbouncer-config
          mountPath: /etc/pgbouncer
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"

      volumes:
      - name: pgbouncer-config
        configMap:
          name: pgbouncer-config
```

---

## GitOps Deployment with Flux CD

### GitRepository Configuration

```yaml
apiVersion: source.toolkit.fluxcd.io/v1
kind: GitRepository
metadata:
  name: synergyflow-repo
  namespace: flux-system
spec:
  interval: 1m
  url: https://github.com/monosense/synergyflow-infra.git
  ref:
    branch: main
  secretRef:
    name: flux-ssh-key
  ignore: |
    # exclude all
    /*
    # include docs
    !/docs
    # include k8s manifests
    !/k8s
    # exclude CI/CD
    .github/
```

### Kustomization with Environment Overlays

```yaml
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: synergyflow-dev
  namespace: flux-system
spec:
  interval: 10m
  path: ./k8s/overlays/dev
  prune: true
  wait: true
  sourceRef:
    kind: GitRepository
    name: synergyflow-repo
  healthChecks:
  - apiVersion: apps/v1
    kind: Deployment
    name: synergyflow-backend
    namespace: default
  - apiVersion: apps/v1
    kind: Deployment
    name: synergyflow-frontend
    namespace: default
  validation: server
```

---

## Resource Sizing Summary

| Component | Requests (CPU/Memory) | Limits (CPU/Memory) | Replicas | Total |
|-----------|-------------------|-----|----------|-------|
| Backend (app) | 500m / 512Mi | 1000m / 1Gi | 3 | 1.5 CPU / 1.5Gi |
| Frontend | 250m / 512Mi | 500m / 1Gi | 3 | 750m / 1.5Gi |
| PostgreSQL | 1000m / 2Gi | 2000m / 4Gi | 3 | 3 CPU / 6Gi |
| PgBouncer | 100m / 128Mi | 200m / 256Mi | 3 | 300m / 384Mi |
| DragonflyDB | 500m / 1Gi | 1000m / 2Gi | 1 (shared) | 500m / 1Gi |
| **TOTAL** | | | | **~5.6 CPU / ~10.8Gi** |

**Note:** This is MVP sizing. Production capacity can be auto-scaled using Kubernetes HPA (Horizontal Pod Autoscaler).

---

## See Also

- **architecture.md Section 10:** Deployment Architecture overview
- **architecture.md Section 1.4:** Technology Decision Table (Kubernetes, PostgreSQL, DragonflyDB versions)
- **CloudNative-PG Docs:** https://cloudnative-pg.io/
- **Envoy Gateway Docs:** https://gateway.envoyproxy.io/
- **Flux CD Docs:** https://fluxcd.io/docs/
