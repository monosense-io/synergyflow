---
id: arch-08-testing-architecture
title: Testing Architecture
owner: QA
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/11-testing-strategy-spring-modulith.md
  - ../prd/24-spring-modulith-architecture-compliance-summary.md
---

## Module Tests

- @ApplicationModuleTest with PublishedEvents/AssertablePublishedEvents
- Scenario-driven tests for cross-module flows

## Architecture Tests

- ApplicationModules.verify() in CI
- Boundary violation tests; event contract checks

## NFR Tests

- Load, failover, DR drills per Sizing Appendix
