---
title: Database Migrations Strategy
owner: Architect (Data)
status: Draft
last_updated: 2025-09-18
links:
  - ../prd/26-db-schema-delta-additions-changes.md
  - ../../migrations/
---

Overview
- Versioned SQL migrations under `migrations/` named `V###__description.sql`.
- Non-destructive by default; phased rollouts when altering existing structures.
- Rollback guidance recorded inline per migration.

Current Migrations
- V001__problems_add_correlation_key.sql
- V002__changes_add_rollback_fields.sql
- V003__services_add_form_schema.sql
- V004__service_requests_add_form_and_approved_by.sql
- V005__articles_add_owner_and_expires.sql
- V006__cmdb_core_tables.sql
- V007__routing_and_skills.sql
- V008__preferences_and_notifications.sql
- V009__change_calendar.sql

Operational Notes
- Validate migrations against Testcontainers PostgreSQL in CI; avoid H2.
- Indexing strategy aligns with PRD 18 (Data Model Overview) performance indexes.
- Document any destructive changes in a superseding ADR prior to execution.

