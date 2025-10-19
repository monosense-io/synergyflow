# 1. Introduction

This document outlines the complete fullstack architecture for **SynergyFlow**, a unified ITSM+PM platform with intelligent workflow automation. It serves as the single source of truth for AI-driven development, ensuring consistency across the entire technology stack.

SynergyFlow implements a **modular monolith** architecture using Spring Modulith, not microservices. This architectural choice provides operational simplicity, development velocity, and cost efficiency for a 5-person team delivering integrated ITSM+PM capabilities without microservices overhead.

## Starter Template or Existing Project

**Status:** Existing Project - Foundation Phase In Progress

**Completed Components (Epic-16 Stories 16.06-16.09):**
- Backend monolith bootstrap (Spring Boot 3.5.6 with Spring Modulith 1.4.2)
- Frontend app bootstrap (Next.js 15.1+ with React 19)
- Backend OAuth2 Resource Server JWT authentication
- Gateway JWT validation (Envoy Gateway with Kubernetes Gateway API)

**Repository:**
- Location: `/Users/monosense/repository/synergyflow`
- Git branch: `main`
- Recent commits: Epic 00 Event System complete, transactional outbox pattern implemented

**Constraints from Existing Decisions:**
- Spring Modulith modular monolith architecture (NOT microservices)
- Shared PostgreSQL cluster pattern (consistent with platform: gitlab, harbor, keycloak, mattermost)
- Shared DragonflyDB cluster for caching
- In-JVM event bus with transactional outbox (NO Kafka - architectural decision, see PRD Out of Scope section)
- Kubernetes deployment with Flux CD 2.2.3 GitOps
- OPA 0.68.0 sidecar for policy evaluation
- Flowable 7.1.0 embedded for BPMN workflows

---
