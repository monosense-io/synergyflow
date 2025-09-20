Status: Draft

# Story
As an approval administrator,
I want per-service approval workflows with multi-stage policies,
so that requests follow the correct governance before fulfillment.

## Acceptance Criteria
1. Associate an approval workflow (Flowable BPMN) to each service/offering; stages can be single or multi-approver with delegation.
2. Execution enforces completion of required approvals before fulfillment tasks start; blocks on pending/denied.
3. All approval actions are audit logged (who, when, rationale); notifications are sent to approvers and requestors.
4. Support policy conditions (amount/role/risk) to select workflow variants.
5. Events: request.approval.requested, request.approved/denied with workflow/version IDs.

## Tasks / Subtasks
- [ ] Workflow association model (service→workflow) (AC: 1)
- [ ] Flowable execution integration and state tracking (AC: 2)
- [ ] Audit and notifications wiring (AC: 3)
- [ ] Policy condition evaluator for workflow selection (AC: 4)
- [ ] Event emission for approval lifecycle (AC: 5)

## Dev Notes
- PRD: 2.6 Service Catalog (multi-stage approvals)
- Architecture: docs/architecture/workflow-flowable.md; gateway-envoy.md
- ADRs: 0012 Flowable orchestration; 0001 event bus; 0004 event versioning; 0007 gateway
- APIs: service-requests.yaml (approval endpoints), system.yaml (policy config)

## Testing
- Workflow selection by policy; approval path success/denial
- Block fulfillment until approvals complete
- Notification delivery; audit integrity tests

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 04 seed #3           | PO     |

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

