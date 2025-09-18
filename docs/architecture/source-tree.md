---
id: arch-11-source-tree
title: Source Tree & Layout (Backend + Frontend)
owner: Architect
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/03-system-architecture-monolithic-approach.md
  - ../prd/11-testing-strategy-spring-modulith.md
  - ../prd/24-spring-modulith-architecture-compliance-summary.md
  - coding-standards.md
  - module-architecture.md
---

## Scope & Constraints
- Documentation‑only specification of the repository layout. Do not scaffold code here.
- Backend must comply with Spring Modulith boundaries and validations.
- Package naming MUST start with `io.monosense.synergyflow`.

## Repository Layout (Top‑Level)
```
synergyflow-backend/        # Spring Modulith monolith (no code here, structure spec below)
synergyflow-frontend/       # Next.js 14 app (no code here, structure spec below)
docs/                       # Documentation (PRD, Architecture, ADRs)
migrations/                 # SQL migrations (versioned)
mkdocs.yml                  # Docs site config
```

## Backend Source Tree (Spring Modulith)

Key rules
- One monolith project with modules (bounded contexts) mapped to Java packages.
- Each module declares `package-info.java` with `@ApplicationModule` and optional `@NamedInterface("api")` for external tooling.
- ITIL modules have zero direct dependencies between each other (ADR‑0001). Only `team → user::api` is allowed (ADR‑0002).
- Tests validate boundaries via `ApplicationModules.verify()` and `@ApplicationModuleTest`.

Proposed structure
```
synergyflow-backend/
  src/
    main/
      java/
        io/monosense/synergyflow/
          user/                         # foundation module (shared)
            package-info.java           # @ApplicationModule
            domain/
            application/
            infrastructure/
          team/
            package-info.java           # @ApplicationModule(allowedDependencies = "user :: api")
            api/                        # @NamedInterface("api") types
            domain/
            application/
            infrastructure/
          incident/
            package-info.java           # @ApplicationModule
            domain/events/              # IncidentCreatedEvent, etc. (records)
            domain/
            application/
            infrastructure/
          problem/
            package-info.java
            domain/events/
            domain/
            application/
            infrastructure/
          change/
            package-info.java
            domain/events/              # ChangeImpactAssessmentRequestedEvent
            domain/
            application/
            infrastructure/
          knowledge/
            package-info.java
            domain/events/
            domain/
            application/
            infrastructure/
          cmdb/
            package-info.java
            api/                        # optional named interface (external tooling)
            domain/events/              # ChangeImpactAssessedEvent
            domain/
            application/
            infrastructure/
          notification/
            package-info.java
            application/
          workflow/
            package-info.java
            application/
          integration/
            package-info.java
            application/                # adapters, gateways, webhooks
      resources/
        application.yml
    test/
      java/
        io/monosense/synergyflow/
          **/*ApplicationModuleTest.java    # Modulith tests per module
          **/*ArchitectureTests.java        # ApplicationModules.verify()
```

Conventions
- Use Lombok for boilerplate (@Value/@Builder/@RequiredArgsConstructor, @Slf4j).
- Use MapStruct for DTO ↔ domain ↔ entity mappings (componentModel="spring").
- Event classes as immutable Java records with `version` and `Instant createdAt`.
- Tests use Testcontainers (PostgreSQL/Redis/MinIO/Kafka); avoid H2.

Validation
- `ApplicationModules.verify()` executed in CI.
- Modulith actuator `/actuator/modulith` enabled for runtime visual inspection.
- High‑level architecture tests assert no forbidden dependencies (e.g., change ↔ cmdb direct calls).

## Frontend Source Tree (Next.js 14)

Principles
- App Router with RSC; use client components only where necessary.
- Feature‑first organization under `features/`.
- Shared UI components in `components/ui` (shadcn/ui), core components in `components/core`.

Proposed structure
```
synergyflow-frontend/
  app/
    (marketing)/
    (app)/
      incidents/
      problems/
      changes/
      cmdb/
      layout.tsx
      page.tsx
  components/
    ui/
    core/
  features/
    incidents/
      api/
      components/
      pages/
    problems/
    changes/
    cmdb/
  lib/
    api-client.ts
    auth.ts
    i18n.ts
  styles/
    globals.css
  public/
```

Tooling & Standards
- TypeScript strict; ESLint + Prettier; Tailwind + shadcn/ui.
- API access via fetch/axios with interceptors; TanStack Query for caching.
- Auth via NextAuth (OIDC/SAML proxied); RBAC on routes.
- Accessibility: WCAG 2.1 AA; keyboard/focus management.

## Getting Started (Doc‑Only)
- Create the directories above when ready to scaffold code; follow Coding Standards and Module Architecture docs.
- Add `package-info.java` to each backend module with `@ApplicationModule` to enable Modulith verification.
- Add `@NamedInterface("api")` subpackages only where explicitly allowed.
- Add `@ApplicationModuleTest` suites and architecture tests before feature code.
