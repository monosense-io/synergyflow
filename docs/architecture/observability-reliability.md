---
id: arch-07-observability-reliability
title: Observability & Reliability
owner: Infra/Platform
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/09-high-availability-reliability-architecture.md
  - ../prd/12-enhanced-performance-reliability-requirements.md
---

## Stack

- Prometheus, Grafana, AlertManager
- ELK for logs; OpenTelemetry for traces
- Modulith actuator

## Practices

- SLOs by module; synthetic checks; DR drills
- Alert routing by severity; escalation policies

## SLOs & Alerting (Draft)

- Incident module: p95 create→event publish < 50ms; read latencies < 150ms.
- Event publication: incomplete publications retry within 30m; backlog < 5k.
- Alerting: page on sustained SLO violation (>15m) or error rate > 2%.
- Runbooks: link per module to triage steps and dashboards.
