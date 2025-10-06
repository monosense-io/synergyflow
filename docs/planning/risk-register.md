# Risk Register — SynergyFlow

**Owner:** Product Manager (PM)
**Date:** 2025-10-06
**Version:** 1.0 (Draft)

---

## Top Risks, Impact, Mitigation

1) Hibernate 7.0 + UUIDv7 adoption
- Likelihood: M | Impact: H
- Mitigation: Follow `docs/uuidv7-implementation-guide.md`; verify ORM version with Gradle task; fall back to custom generator if needed.

2) Keycloak integration complexity
- Likelihood: M | Impact: H
- Mitigation: Start with resource server JWT validation; stub realms in dev; add batch auth only after CRUD steady.

3) Redis Streams/SSE fan‑out tail latency
- Likelihood: M | Impact: H
- Mitigation: Back‑pressure, bounded payloads, idempotent reducers; load test in S6 and S9.

4) Timer precision & SLA correctness
- Likelihood: M | Impact: H
- Mitigation: Business calendar model, O(1) update/cancel; conformance tests; monitor drift metric.

5) OpenSearch operational overhead
- Likelihood: M | Impact: M
- Mitigation: Single hot node + replica; index only essentials; defer complex analyzers until S9.

6) Holiday capacity dips (Nov/Dec)
- Likelihood: H | Impact: M
- Mitigation: S6 hardening scope; avoid new large features; pull small backlog items only.

7) Performance budgets not met
- Likelihood: M | Impact: H
- Mitigation: Perf budgets as gates at S6/S9; add Gatling scenarios; enforce payload discipline.

8) Scope creep on workflow intelligence
- Likelihood: M | Impact: M
- Mitigation: Lock innovation FRs to MVP; simulate with stub data; expand post‑GA.

9) Environment flakiness blocking QA
- Likelihood: M | Impact: M
- Mitigation: Health pings; gating checks before “Done”; ephemeral envs for E2E.

10) Security/compliance gaps late
- Likelihood: L | Impact: H
- Mitigation: Early security review; audit log MVP in S7; OWASP/SAST in CI.

