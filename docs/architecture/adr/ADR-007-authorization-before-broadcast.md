# ADR 0007 — Authorization‑Before‑Broadcast Pattern

Date: 2025-10-08
Status: Accepted
Owner: Developer Agent
Priority: High
Applies To: SSE broadcaster, WebSocket (future), subscription layer

## Context
Realtime fan‑out risks leaking data across tenants/roles if messages are broadcast without per‑subscriber checks. Our SSE bridge fans out domain events to many clients with different `tenant_id`, `project_scope`, and roles.

## Decision
Evaluate authorization and scope filtering before broadcasting each message to a subscriber. The broadcaster must:

1) Resolve a connection’s immutable `SubscriptionContext` from the security principal at connect time (`tenant_id`, `user_id`, `roles`, `project_ids`).
2) Transform domain events into projection payloads containing only fields permitted for that context.
3) Deliver only events that pass `authorize(event, context)`; drop others silently.

### Pseudocode (Java)
```java
public final class SubscriptionContext {
  private final UUID tenantId;
  private final UUID userId;
  private final Set<String> roles;
  private final Set<UUID> projects;

  public SubscriptionContext(UUID tenantId, UUID userId, Set<String> roles, Set<UUID> projects) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.roles = roles;
    this.projects = projects;
  }

  public UUID getTenantId() { return tenantId; }
  public UUID getUserId() { return userId; }
  public Set<String> getRoles() { return roles; }
  public Set<UUID> getProjects() { return projects; }
}

static boolean authorize(Event e, SubscriptionContext ctx) {
  return Objects.equals(e.getTenantId(), ctx.getTenantId())
      && (e.getProjectId() == null || ctx.getProjects().contains(e.getProjectId()));
}

static void onEvent(Event e, Collection<Connection> connections) {
  for (Connection c : connections) {
    SubscriptionContext ctx = c.getContext();
    if (authorize(e, ctx)) {
      c.send(toProjection(e, ctx));
    }
  }
}
```

### Additional Requirements
- Do not broadcast raw domain entities. Use projection DTOs that enforce field‑level visibility.
- Cache coarse‑grained decisions (e.g., role → event types) for up to 60s to reduce CPU, but never cache tenant/user equality checks.
- Log denials only at debug level with structured keys (no PII). Expose counters: `sse_events_authorized_total`, `sse_events_denied_total`.
- On reconnection/replay, apply the same authorization checks over the backfilled range.

## Consequences
- Eliminates cross‑tenant/role leakage risks in realtime channels.
- Adds small CPU overhead proportional to connections × events; mitigated by caching coarse decisions and using efficient filters.

## Alternatives Considered
- Broadcast‑then‑filter on client: unacceptable—data already leaked on the wire.
- Per‑topic queues per tenant/project: secure but operationally heavy at current scale; revisit if fan‑out becomes a bottleneck.

## References
- docs/architecture/solution-architecture.md
- docs/architecture/eventing-and-timers.md
