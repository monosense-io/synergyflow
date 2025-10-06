# Epic Alignment Matrix

| Epic | Spec | Components | Key APIs | Read Models |
|---|---|---|---|---|
| Epic 1 — Foundation | docs/epics/epic-1-foundation-tech-spec.md | Security, Eventing, SLA, SSE, Audit | /actuator/health | outbox, ticket_card, issue_card |
| Epic 2 — ITSM | docs/epics/epic-2-itsm-tech-spec.md | ITSM module, SSE | docs/api/itsm-api.yaml → /api/itsm/tickets, /api/itsm/console/queue, /api/itsm/console/sidepanel/{id} | ticket_card |
| Epic 3 — PM | docs/epics/epic-3-pm-tech-spec.md | PM module, SSE | docs/api/pm-api.yaml → /api/pm/issues, /api/pm/boards/{boardId}, /api/pm/sprints/{sprintId}/burndown | issue_card, board_view, sprint_summary |
| Epic 4 — Workflow | docs/epics/epic-4-workflow-tech-spec.md | Workflow, SLA, Security | docs/api/workflow-api.yaml → /api/workflow/inbox, /api/workflow/approvals/{requestId}, /api/workflow/rules, /api/workflow/simulate, /api/auth/batchCheck | approval_inbox, braided_timeline, pbi_card |

Notes
- API contracts live under docs/api/*.yaml. See docs/api/README.md for versioning and cursor/replay semantics.
- Read model definitions: docs/architecture/data-model.md.
