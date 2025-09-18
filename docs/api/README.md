---
title: API Reference
owner: Architect (Integration)
status: Draft
last_updated: 2025-09-18
links:
  - ../architecture/api-architecture.md
---

OpenAPI

- Aggregated spec: openapi.yaml (built from modules by scripts/build_openapi.py)
- Per-module specs: see files under api/modules/

Usage

- Generate clients with OpenAPI Generator or similar tools.
- Base path: `/api/v1`.
- Security: bearer (JWT) or OAuth2; API key supported for service-to-service.
- Errors: `{ code, message, details, correlationId }`.
