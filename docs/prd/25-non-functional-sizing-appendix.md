---
id: 25-non-functional-sizing-appendix
title: 25. Non‑Functional Sizing Appendix
version: 8.0
last_updated: 2025-09-03
owner: Infra/Platform
status: Draft
---

## 25. Non‑Functional Sizing Appendix

Log retention sizing

- Audit logs: 7-year retention. Estimate storage via daily ingest × 365 × 7.
- Daily ingest (GB) ≈ (events/day × avg_event_size_bytes) / 1e9.
- Example worksheet: events/day = (avg TPS × 86,400) × event_externalization_ratio.
- Recommendation: tiered storage (hot 30 days, warm 6–12 months, cold archive thereafter) with lifecycle policies.

Event/TPS modeling vs concurrency

- Peak user concurrency: 350. Derive peak app TPS from user actions + background jobs.
- TPS breakdown: interactive (UI/API) + async (events, schedulers, integrations).
- Capacity planning: size for p95 peak × 1.5 headroom; validate with load tests.
- If targeting 2000+ TPS event publication, set registry retention ≤ 7 days and enforce daily archive/cleanup.

Network egress allowances

- Mobile push: allow outbound to FCM/APNs endpoints.
- Email delivery: SMTP relay/proxy egress.
- BI/analytics and webhooks: allowlist per integration.
- GraphQL subscriptions/SSE (if external): document ports and proxies.

Storage baselines (initial)

- Object storage: 1TB initial; validate against audit retention projections; add archive tier.
- Database: partitioned event_publication; monitor monthly partition growth and vacuum.

## Review Checklist

- Content complete and consistent with PRD
- Acceptance criteria traceable to tests (where applicable)
- Data model references validated (where applicable)
- Event interactions documented (where applicable)
- Security/privacy considerations addressed
- Owner reviewed and status updated

## Traceability

- Features → Data Model: see 18-data-model-overview.md
- Features → Events: see 03-system-architecture-monolithic-approach.md
- Features → Security: see 13-security-compliance-framework.md
- Features → Tests: see 11-testing-strategy-spring-modulith.md
