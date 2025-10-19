# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) documenting significant architectural decisions made for the SynergyFlow project.

## What is an ADR?

An Architecture Decision Record (ADR) is a document that captures an important architectural decision made along with its context and consequences.

## Format

We use a simplified MADR (Markdown Any Decision Records) format:

- **Title**: Short noun phrase describing the decision
- **Status**: Accepted | Proposed | Deprecated | Superseded
- **Date**: When the decision was made
- **Context**: Forces at play, background, problem statement
- **Decision**: What we decided to do and why
- **Consequences**: Positive and negative outcomes
- **Alternatives Considered**: Other options evaluated

## Index of ADRs

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0001](./0001-spring-modulith-over-microservices.md) | Spring Modulith over Microservices | Accepted | 2025-10-18 |
| [0002](./0002-shared-postgresql-cluster-pattern.md) | Shared PostgreSQL Cluster Pattern | Accepted | 2025-10-18 |
| [0003](./0003-spring-data-jpa-over-mybatis.md) | Spring Data JPA over MyBatis | Accepted | 2025-10-18 |
| [0004](./0004-nextjs-15-react-19-latest-stable.md) | Next.js 15 & React 19 Latest Stable | Accepted | 2025-10-18 |
| [0005](./0005-shadcn-ui-over-chakra-or-mui.md) | Shadcn/ui over Chakra UI or MUI | Accepted | 2025-10-18 |
| [0006](./0006-no-kafka-in-jvm-event-bus.md) | No Kafka - In-JVM Event Bus (Spring Modulith) | Accepted | 2025-10-18 |
| [0007](./0007-opa-sidecar-for-policy-engine.md) | OPA Sidecar for Policy Engine | Accepted | 2025-10-18 |
| [0008](./0008-flowable-for-workflow-engine.md) | Flowable for Workflow Orchestration | Accepted | 2025-10-18 |

## References

- [MADR Template](https://adr.github.io/madr/)
- [ADR GitHub Organization](https://adr.github.io/)
- SynergyFlow Architecture Documentation: [docs/architecture/](../architecture/)
