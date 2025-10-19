# 13. Monitoring and Observability

## 13.1 Metrics Collection (Victoria Metrics + Grafana)

### Victoria Metrics Scrape Configuration

```yaml
# infrastructure/base/vmagent-scrape-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: vmagent-scrape-config
  namespace: monitoring
data:
  scrape.yaml: |
    global:
      scrape_interval: 15s

    scrape_configs:
    - job_name: 'synergyflow-backend'
      kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
          - synergyflow
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: synergyflow-backend
        action: keep
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: pod
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
      metrics_path: /actuator/prometheus

    - job_name: 'synergyflow-frontend'
      kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
          - synergyflow
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: synergyflow-frontend
        action: keep
      metrics_path: /api/metrics
```

### Grafana Dashboard (API Performance)

**Key Metrics Tracked:**
- `http_server_requests_seconds_bucket` - API latency histogram (p50, p95, p99)
- `http_server_requests_total` - Request rate (req/s)
- `event_processing_lag_seconds` - Projection lag from event_publication table
- `policy_evaluation_duration_seconds` - OPA evaluation latency
- `hikaricp_connections_active` - Database connection pool utilization
- `jvm_memory_used_bytes` - JVM heap usage

**Alert Rules:**
```yaml
# infrastructure/base/alerts.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: alerting-rules
  namespace: monitoring
data:
  rules.yaml: |
    groups:
    - name: synergyflow_alerts
      interval: 30s
      rules:
      - alert: HighAPILatency
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket{job="synergyflow-backend"}) > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API latency p95 > 200ms"

      - alert: HighEventProcessingLag
        expr: event_processing_lag_seconds > 0.5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Event processing lag > 500ms"

      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Error rate > 1%"
```

## 13.2 Distributed Tracing (OpenTelemetry)

### Spring Boot OpenTelemetry Configuration

```yaml
# backend/src/main/resources/application.yaml
management:
  tracing:
    sampling:
      probability: 0.1 # 10% sampling in production
  otlp:
    tracing:
      endpoint: http://tempo.monitoring.svc:4317
```

**Correlation ID Propagation:**
```java
// shared/utils/CorrelationIdFilter.java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

---
