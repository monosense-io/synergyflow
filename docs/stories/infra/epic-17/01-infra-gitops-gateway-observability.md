Status: Draft

# Epic 17 — Infra: GitOps & Gateway Observability
As a platform engineer,
I want GitOps pipelines and baseline observability for the Envoy Gateway,
so that gateway policy changes are safely promoted and operational health is visible.

## Acceptance Criteria
1. GitOps delivery: A repo path and CI bootstrap that continuously applies `infra/gateway/**` via environment overlays (dev→stg→prod) with approvals.
2. Policy checks in CI: yamllint, kubeconform (ignore missing CRDs), and Conftest (Rego) pass before promotion.
3. Dashboards: 401/403 rates, JWT error breakdown, JWKS fetch errors, latency P50/P95, rate-limit hits (per route).
4. Alerts: JWKS outage (SLO), 401/403 surge, 5xx surge, latency breach; runbook links included.
5. Documentation: GitOps flow, promotion approvals, rollback, and dashboard links documented.

## Tasks / Subtasks
- [ ] Create GitOps workflow(s) to render/apply overlays (`infra/gateway/overlays/{dev,stg,prod}`) with approvals
- [ ] Integrate Conftest policy checks into CI (blocking)
- [ ] Add dashboards for gateway metrics; export JSON definitions
- [ ] Add alerts for outage/surge/latency; export alert rules
- [ ] Document GitOps flow and runbooks under `docs/architecture/gateway-envoy.md`

## Dev Notes
- Use Kustomize overlays added in 16.09 follow-up.
- Consider Argo CD or Flux for pull-based GitOps; wire approval gates in CI.
- Emit metrics from Envoy Gateway; scrape with Prometheus; visualize in Grafana.

## Testing
- CI dry-run with overlays; Conftest pass/fail scenarios
- Dashboard rendering sanity; alert rule firing in a simulated outage

## Change Log
| Date       | Version | Description                                    | Author |
|------------|---------|------------------------------------------------|--------|
| 2025-09-21 | 0.1     | Initial draft — GitOps & gateway observability | PO     |

## Dev Agent Record

### Agent Model Used
<record at implementation time>

### Debug Log References
<links at implementation time>

### Completion Notes List
<notes at implementation time>

### File List
<files at implementation time>

## QA Results
<QA to fill>

