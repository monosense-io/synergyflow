# Validation Report — Senior Developer Review

**Document:** docs/stories/story-00.2.md
**Checklist:** bmad/bmm/workflows/4-implementation/review-story/checklist.md
**Date:** 2025-10-18

## Summary
- Overall: PASS with Changes Requested (action items logged)
- Critical Issues: 2 (E2E selector mismatch; optimistic ID bug)

## Section Results

✓ Story file loaded: docs/stories/story-00.2.md
✓ Status verified: Ready for Review → Set to InProgress after review (changes requested)
✓ Epic/Story IDs: 00.2 resolved from filename
✓ Story Context located: docs/stories/story-context-00.00.2.xml
✓ Tech Spec: none (warning acceptable; architecture + PRD used)
✓ Architecture/standards docs: loaded (CODING-STANDARDS.md, architecture.md, testing strategy)
✓ Tech stack: Spring Boot/Modulith/PostgreSQL; Next.js/TypeScript/React Query/Zustand

Acceptance Criteria Cross‑check
- AC#1: UI implements optimistic flow; retry path in mutation present. Evidence: apps/frontend/src/features/time/tray/TimeTray.tsx L44–115.
- AC#2: Mirroring to ≥2 entities with audit trail. Evidence: consumers and schema files; IncidentWorklogConsumer.java L59–118; V004__time_entry_schema.sql L1–220.
- AC#3: Aggregates update; freshness badges model present. Evidence: useTimeTray.ts L70–111.
- AC#4: Bulk time entry supported. Evidence: TimeEntryController.java L86–113; API client createBulkTimeEntries.
- AC#5: p95 ≤200ms: k6 script with thresholds. Evidence: apps/backend/src/test/k6/time-entry-performance-test.js L1–200.

File List reviewed
- Files listed in story exist (spot‑checked key items). Evidence via ripgrep listing under apps/backend and apps/frontend.

Tests mapping
- Backend unit/integration tests present (time entry service; incident outbox/consumers exist). E2E test present but selectors missing in UI (see findings).

Code Quality
- No obvious SQL injection; prepared statements used.
- RFC7807 responses implemented in controller; correlationId included.
- Minor: `userId = "test-user"` placeholder for JWT.

Security Review
- Auth placeholder acceptable for MVP; ensure follow‑up to integrate JWT and OPA policies.

Outcome
- Changes Requested — see Action Items.

## Failed/Partial Items
- ✗ UI/E2E selector mismatch — tests reference `data-testid` selectors not present in TimeTray.tsx.
- ✗ Optimistic update ID mismatch causing non‑deterministic confirmation.

## Recommendations / Action Items
1. Add data‑testids to UI; align names to tests. [High]
2. Fix optimistic update ID replacement using onMutate context. [High]
3. Backend E2E/integration test to assert mirrored rows + aggregates. [Medium]
4. JWT extraction for userId when auth enabled. [Medium]

_Reviewer: Eko Purwanto on 2025-10-18_
