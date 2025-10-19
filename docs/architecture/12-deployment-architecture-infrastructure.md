# 12. Deployment Architecture & Infrastructure

## 12.1 Kubernetes Deployment Manifests

### Backend Deployment

```yaml
# infrastructure/base/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: synergyflow-backend
  namespace: synergyflow
  labels:
    app: synergyflow-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: synergyflow-backend
  template:
    metadata:
      labels:
        app: synergyflow-backend
    spec:
      containers:
      - name: backend
        image: harbor.example.com/synergyflow/backend:latest
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: synergyflow-db-credentials
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: synergyflow-db-credentials
              key: password
        - name: SPRING_DATA_REDIS_HOST
          value: "dragonfly.dragonfly-system.svc"
        - name: SPRING_DATA_REDIS_PORT
          value: "6379"
        - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
          value: "https://keycloak.example.com/realms/synergyflow"
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "8Gi"
            cpu: "4"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 60"] # Graceful shutdown (60s grace period)

      - name: opa-sidecar
        image: openpolicyagent/opa:0.68.0
        ports:
        - containerPort: 8181
          name: http
        args:
        - "run"
        - "--server"
        - "--addr=0.0.0.0:8181"
        - "/policies"
        volumeMounts:
        - name: opa-policies
          mountPath: /policies
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "500m"

      volumes:
      - name: opa-policies
        configMap:
          name: opa-policies

      terminationGracePeriodSeconds: 60
---
apiVersion: v1
kind: Service
metadata:
  name: synergyflow-backend
  namespace: synergyflow
spec:
  selector:
    app: synergyflow-backend
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  type: ClusterIP
```

### Frontend Deployment

```yaml
# infrastructure/base/frontend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: synergyflow-frontend
  namespace: synergyflow
  labels:
    app: synergyflow-frontend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: synergyflow-frontend
  template:
    metadata:
      labels:
        app: synergyflow-frontend
    spec:
      containers:
      - name: frontend
        image: harbor.example.com/synergyflow/frontend:latest
        ports:
        - containerPort: 3000
          name: http
        env:
        - name: NEXT_PUBLIC_API_URL
          value: "https://synergyflow.example.com/api/v1"
        - name: NEXTAUTH_URL
          value: "https://synergyflow.example.com"
        - name: NEXTAUTH_SECRET
          valueFrom:
            secretKeyRef:
              name: nextauth-secret
              key: secret
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /api/health
            port: 3000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/health
            port: 3000
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: synergyflow-frontend
  namespace: synergyflow
spec:
  selector:
    app: synergyflow-frontend
  ports:
  - name: http
    port: 3000
    targetPort: 3000
  type: ClusterIP
```

### Envoy Gateway Configuration

```yaml
# infrastructure/base/gateway.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: synergyflow-gateway
  namespace: synergyflow
spec:
  gatewayClassName: envoy
  listeners:
  - name: http
    protocol: HTTP
    port: 80
    hostname: "synergyflow.example.com"
  - name: https
    protocol: HTTPS
    port: 443
    hostname: "synergyflow.example.com"
    tls:
      mode: Terminate
      certificateRefs:
      - name: synergyflow-tls-cert
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: frontend-route
  namespace: synergyflow
spec:
  parentRefs:
  - name: synergyflow-gateway
  hostnames:
  - "synergyflow.example.com"
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: /api
    backendRefs:
    - name: synergyflow-backend
      port: 8080
  - matches:
    - path:
        type: PathPrefix
        value: /
    backendRefs:
    - name: synergyflow-frontend
      port: 3000
---
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: SecurityPolicy
metadata:
  name: jwt-validation
  namespace: synergyflow
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: frontend-route
  jwt:
    providers:
    - name: keycloak
      issuer: "https://keycloak.example.com/realms/synergyflow"
      audiences:
      - "synergyflow-backend"
      remoteJWKS:
        uri: "https://keycloak.example.com/realms/synergyflow/protocol/openid-connect/certs"
---
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: BackendTrafficPolicy
metadata:
  name: rate-limit-policy
  namespace: synergyflow
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: frontend-route
  rateLimit:
    type: Global
    global:
      rules:
      - limit:
          requests: 100
          unit: Minute
```

## 12.2 GitOps with Flux CD

### Flux Configuration

```yaml
# infrastructure/flux/kustomization.yaml
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: synergyflow
  namespace: flux-system
spec:
  interval: 5m
  path: ./infrastructure/overlays/production
  prune: true
  sourceRef:
    kind: GitRepository
    name: synergyflow-infra
  validation: client
  healthChecks:
  - apiVersion: apps/v1
    kind: Deployment
    name: synergyflow-backend
    namespace: synergyflow
  - apiVersion: apps/v1
    kind: Deployment
    name: synergyflow-frontend
    namespace: synergyflow
```

### Environment Overlays

```yaml
# infrastructure/overlays/production/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: synergyflow

bases:
- ../../base

replicas:
- name: synergyflow-backend
  count: 3
- name: synergyflow-frontend
  count: 3

images:
- name: harbor.example.com/synergyflow/backend
  newTag: v1.0.0
- name: harbor.example.com/synergyflow/frontend
  newTag: v1.0.0

configMapGenerator:
- name: backend-config
  literals:
  - SPRING_PROFILES_ACTIVE=production
  - LOGGING_LEVEL_ROOT=INFO
```

---
