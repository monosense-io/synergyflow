---
title: Contributing to Documentation
owner: Product
status: Draft
last_updated: 2025-09-18
---

Scope
- Standards for adding and updating documentation in `docs/`.

Front-Matter
- Include: title, owner, status, last_updated, and links (when relevant).

Status Workflow
- Draft → In Review → Accepted. Assign an owner and reviewers per shard.

Style & Structure
- Prefer short sections; link to related shards and ADRs.
- Keep cross-links relative within `docs/` to pass strict builds.

Process
- Update related shards on changes (Architecture, PRD, ADRs).
- For significant changes, add or update an ADR.
- Run `mkdocs build --strict` locally before PR.

Quality Gates (CI)
- MkDocs strict build must pass.
- Markdownlint no errors.
- Link check must pass or be justified.

