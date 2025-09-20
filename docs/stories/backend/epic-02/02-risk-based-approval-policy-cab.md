Status: Draft

# Story
As a CAB coordinator,
I want risk-based approval policies with enforced sign-off,
so that change approvals match risk levels and are fully auditable.

## Acceptance Criteria
1. Calculate a risk profile for submitted changes using configurable factors (impact, CI criticality, change type) and return a risk tier (low/medium/high).
2. Enforce approval policies by tier (e.g., CAB required for medium/high), blocking deployment until approvals complete.
3. Require a documented rollback plan for medium/high risk before approval is granted.
4. Record immutable approval decisions (who, when, result, rationale) with an audit trail; expose for reporting.
5. Emit events: change.submitted, change.approval.requested, change.approved/denied with risk tier.

## Tasks / Subtasks
- [ ] RiskProfile service and policy configuration (AC: 1)
- [ ] Approval workflow integration (AC: 2)
- [ ] Rollback plan validation for med/high (AC: 3)
- [ ] Audit logging for approvals (AC: 4)
- [ ] Event emissions for submission/approval lifecycle (AC: 5)
- [ ] RBAC/ABAC checks for approvers (AC: 2, 4)

## Dev Notes
- PRD 2.3 Change Management (risk assessment, CAB approvals, rollback plan)
- Dependencies: CMDB impact signal (docs/adrs/0005-cmdb-impact-assessment-handshake.md), Security (RBAC) docs/epics/epic-12-security-compliance/README.md
- ADRs: 0001 event bus; 0004 event versioning; 0005 CMDB handshake; 0009 DB migrations policy
- APIs: changes.yaml for submission/approval endpoints; system.yaml for policy config
- Data: Change, RiskProfile, Approval, RollbackPlan, AuditLog

## Testing
- Risk tiering rules unit tests; policy evaluation tests
- Enforced blocking without approvals for med/high
- Rollback plan required checks
- Event contract tests and audit immutability

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 02 seed #2          | PO     |

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

