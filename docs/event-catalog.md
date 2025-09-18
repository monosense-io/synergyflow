# Event Catalog

This catalog summarizes key domain events, producers, consumers, and intent.
See [System Architecture (Monolithic Approach)](prd/03-system-architecture-monolithic-approach.md)
for code examples and patterns.

## Core Events

- IncidentCreatedEvent
  - Producer: itsm.incident
  - Consumers: notification, workflow, problem (pattern analysis)
  - Intent: Notify modules of new incident; start SLA timers; route to team
  - Versioning: 1.0

- IncidentAssignedEvent
  - Producer: itsm.incident
  - Consumers: notification, workflow
  - Intent: Assignment updates and workload tracking

- IncidentResolvedEvent
  - Producer: itsm.incident
  - Consumers: notification, workflow, problem (correlation updates)
  - Intent: Resolution, CSAT prompts, correlation closure

- SlaBreachedEvent / SlaWarningEvent
  - Producer: itsm.incident (SLA engine)
  - Consumers: notification, workflow, reporting
  - Intent: Escalation triggers and stakeholder alerts

- ProblemCreatedEvent
  - Producer: itsm.problem
  - Consumers: incident, knowledge, reporting
  - Intent: Track root-cause work and correlate incidents

- RootCauseIdentifiedEvent
  - Producer: itsm.problem
  - Consumers: knowledge, reporting
  - Intent: Capture RCA for dissemination

- KnownErrorCreatedEvent
  - Producer: itsm.problem
  - Consumers: incident, knowledge
  - Intent: Provide workaround references

- ChangeRequestedEvent / ChangeApprovedEvent / ChangeRejectedEvent / ChangeDeployedEvent
  - Producer: itsm.change
  - Consumers: notification, workflow, reporting
  - Intent: Approval flow, scheduling, deployment status

- ChangeImpactAssessmentRequestedEvent / ChangeImpactAssessedEvent
  - Producer: change (request), cmdb (assessed)
  - Consumers: change, notification, workflow
  - Intent: Event-driven impact gating for approvals

- CICreatedEvent / CIRelationshipChangedEvent / AssetUpdatedEvent
  - Producer: itsm.cmdb
  - Consumers: change, incident, reporting
  - Intent: Reflect CI lifecycle and relationship changes

- ArticleCreatedEvent / ArticleUpdatedEvent / ArticleSearchedEvent / ArticleExpiredEvent
  - Producer: itsm.knowledge
  - Consumers: incident, reporting
  - Intent: Knowledge lifecycle and relevance tracking

- UserCreatedEvent / UserActivatedEvent / UserDeactivatedEvent
  - Producer: itsm.user
  - Consumers: integration, team, notification
  - Intent: Provisioning and identity lifecycle

- TeamCreatedEvent / TeamMemberAssignedEvent / SkillUpdatedEvent / UserAssignedToTeamEvent
  - Producer: itsm.team
  - Consumers: notification, routing, reporting
  - Intent: Organizational structure and routing inputs

- MobileInteractionEvent
  - Producer: mobile API adapter
  - Consumers: analytics, reporting
  - Intent: Record mobile app interactions for analytics

Notes

- Routing keys and headers: see EventExternalizationConfig in the Architecture shard.
- SLAs and latency targets: see 12-enhanced-performance-reliability-requirements.md.
