# ADR 0002: Monolithic Vite App With Internal Routing

Status: Accepted
Date: 2025-10-06
Deciders: Architecture Group
Impacted areas: Frontend structure, CI, DX

## Context

Some notes referenced NPM workspaces (itsm-ui, pm-ui, shared-ui) while others showed a single `frontend/` app. Multiple workspaces increase initial complexity without clear near-term benefits, and CI becomes heavier.

## Decision

Adopt a single Vite app under `frontend/` with route trees and a shared library:

- Routes: `/itsm/*` and `/pm/*` encapsulate module UIs.
- Shared UI: `frontend/src/shared-ui/` for components, hooks, and styles.
- Keep internal structure (`itsm-ui`, `pm-ui`, `shared-ui`) as folders, not separate packages.
- Option to promote to workspaces later (Epics 3–5) with minimal disruption.

## Consequences

- Simpler dev server, bundling, and CI steps.
- Single package.json; consistent Node/Vite versions.
- Tree-shaking shared-ui must be monitored for bundle size; enforce lazy routes where needed.

## Alternatives Considered

- pnpm/Yarn workspaces: clearer ownership boundaries but heavier bootstrap and version drift risk at Phase 1.

## References

- docs/epics/epic-1-foundation-tech-spec.md
- docs/architecture/solution-architecture.md

