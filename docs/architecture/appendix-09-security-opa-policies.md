# Appendix: Section 9 - Security Architecture - OPA Policies and Authorization

This document contains full OPA policy implementations, authorization manager code, and audit logging patterns referenced in `architecture.md` Section 9.

## OPA Policy Bundle - Authorization Policies

For architectural pattern overview, see `architecture.md` Section 9.2 "Authorization and Policy Engine".

### synergyflow/authorization/main.rego

```rego
package synergyflow.authorization

import future.keywords

# Default deny
default allow := false

# User can update incidents they own
allow if {
    input.action == "update"
    input.resource.type == "incident"
    input.resource.ownerId == input.user.id
}

# User with INCIDENT_MANAGER role can update any incident
allow if {
    input.action == "update"
    input.resource.type == "incident"
    "INCIDENT_MANAGER" in input.user.roles
}

# User can view incidents assigned to them
allow if {
    input.action == "read"
    input.resource.type == "incident"
    input.resource.assignedTo == input.user.id
}

# User can view incidents for their team
allow if {
    input.action == "read"
    input.resource.type == "incident"
    input.resource.teamId in input.user.teams
}

# Generate decision receipt
decision := {
    "allow": allow,
    "reason": reason
}

reason := "User owns incident" if {
    allow
    input.resource.ownerId == input.user.id
}

reason := "User has INCIDENT_MANAGER role" if {
    allow
    "INCIDENT_MANAGER" in input.user.roles
}

reason := "Insufficient permissions" if {
    not allow
}
```

---

## OPA Authorization Manager Integration (Spring Security)

### user/infrastructure/OpaAuthorizationManager.java

```java
@Component
public class OpaAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final RestTemplate opaClient;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication,
                                       MethodInvocation invocation) {
        // Build OPA input
        Map<String, Object> input = Map.of(
            "user", extractUser(authentication.get()),
            "resource", extractResource(invocation),
            "action", extractAction(invocation)
        );

        // Call OPA
        OpaResponse response = opaClient.postForObject(
            "http://localhost:8181/v1/data/synergyflow/authorization/decision",
            Map.of("input", input),
            OpaResponse.class
        );

        // Log decision
        auditService.logPolicyDecision(response.getDecision());

        return new AuthorizationDecision(response.isAllow());
    }
}
```

---

## Audit Pipeline - Tamper-Proof Event Logging

For architectural pattern, see `architecture.md` Section 9.3 "Audit Pipeline".

### Audit Event Schema

```sql
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    change_summary JSONB,
    decision_receipt JSONB,  -- For policy decisions
    policy_version VARCHAR(50),
    correlation_id UUID,
    signature_hash VARCHAR(256),  -- HMAC-SHA256 of previous entry
    signature_timestamp TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_audit_user_timestamp ON audit_events(user_id, timestamp DESC);
CREATE INDEX idx_audit_correlation ON audit_events(correlation_id);
```

### Tamper-Proof Logging Implementation

```java
@Service
public class AuditService {
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private EncryptionService encryptionService;

    private String previousSignature = "";

    @Transactional
    public void logPolicyDecision(PolicyDecisionData decision) {
        AuditEvent event = AuditEvent.builder()
            .id(UUID.randomUUID())
            .timestamp(LocalDateTime.now())
            .userId(decision.getUserId())
            .action("POLICY_DECISION")
            .resourceType(decision.getResourceType())
            .resourceId(decision.getResourceId())
            .decisionReceipt(decision.getDecision())  // {allow, reason}
            .policyVersion(decision.getPolicyVersion())
            .correlationId(decision.getCorrelationId())
            .build();

        // Sign with HMAC-SHA256 (chain to previous entry)
        String signature = encryptionService.hmacSha256(
            event.toString() + previousSignature
        );
        event.setSignatureHash(signature);
        event.setSignatureTimestamp(LocalDateTime.now());

        auditEventRepository.save(event);
        previousSignature = signature;  // Update for next entry
    }
}
```

---

## Policy Evaluation Request/Response

### Request Example

```bash
curl -X POST http://localhost:8181/v1/data/synergyflow/authorization/decision \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "user": {
        "id": "user-123",
        "roles": ["INCIDENT_MANAGER"],
        "teams": ["team-ops", "team-platform"]
      },
      "resource": {
        "type": "incident",
        "ownerId": "user-456",
        "assignedTo": "user-123",
        "teamId": "team-ops"
      },
      "action": "update"
    }
  }'
```

### Response Example

```json
{
  "result": {
    "allow": true,
    "reason": "User has INCIDENT_MANAGER role"
  }
}
```

---

## Decision Receipts Structure

Every policy evaluation generates a structured decision receipt (stored in audit pipeline):

```json
{
  "timestamp": "2025-10-17T14:32:45.123Z",
  "userId": "user-123",
  "policyVersion": "authorization/1.2.3",
  "resourceType": "incident",
  "action": "update",
  "input": {
    "user": {
      "id": "user-123",
      "roles": ["INCIDENT_MANAGER"],
      "teams": ["team-ops"]
    },
    "resource": {
      "type": "incident",
      "ownerId": "user-456",
      "assignedTo": "user-123",
      "teamId": "team-ops"
    },
    "action": "update"
  },
  "decision": {
    "allow": true,
    "reason": "User has INCIDENT_MANAGER role"
  },
  "executionTimeMs": 12,
  "correlationId": "corr-abc-123-def"
}
```

**Usage:** Decision receipts enable:
- **Compliance:** "Why was this decision made?" for governance teams
- **Debugging:** Policy developers trace policy evaluation logic
- **Audit Trail:** Tamper-proof log of all authorization decisions
- **User Trust:** Explainable automation (users see decision reasoning)

---

## Shadow Mode Testing

New policies can be deployed in "Shadow Mode" to validate decisions without enforcing them:

**Deployment Steps:**

1. Policy deployed with `shadow: true` annotation
2. OPA evaluates policy but returns `{"allow": true}` (passthrough - allow all)
3. Actual decision logged to audit pipeline with `shadow: true` flag
4. Change manager reviews shadow logs for 2-4 weeks:
   - Compare "what would have been approved" vs actual outcomes
   - Calculate agreement rate (target ≥95%)
5. If quality validated, promote policy to production (remove `shadow: true`)

**Shadow Mode Audit Query:**

```sql
SELECT
  COUNT(*) FILTER (WHERE decision_receipt->>'allow' = 'true') as would_approve,
  COUNT(*) FILTER (WHERE decision_receipt->>'allow' = 'false') as would_deny,
  COUNT(*) as total,
  ROUND(100.0 * COUNT(*) FILTER (WHERE decision_receipt->>'allow' = 'true') / COUNT(*), 2) as approval_rate
FROM audit_events
WHERE policy_version LIKE '%shadow%'
  AND timestamp > NOW() - INTERVAL '2 weeks'
  AND action = 'POLICY_DECISION'
GROUP BY policy_version;
```

---

## Policy Deployment Process

Policies are versioned and deployed via CI/CD pipeline:

1. Developer commits `.rego` file to `synergyflow-backend/src/main/opa/policies/`
2. CI pipeline validates policy syntax: `opa parse policies/*.rego`
3. Policy bundle published to Harbor registry as `.tar.gz`
4. Kubernetes deployment updates OPA ConfigMap with new bundle
5. OPA sidecar automatically reloads policies from ConfigMap
6. Application pods update to use new policy version via rolling restart
7. First policy evaluation uses new version (no downtime)

---

## See Also

- **architecture.md Section 9.2:** OPA policy engine integration patterns
- **architecture.md Section 9.3:** Audit pipeline and tamper-proof logging
- **OPA Documentation:** https://www.openpolicyagent.org/docs/latest/
- **Java HMAC Implementation:** https://docs.oracle.com/javase/tutorial/security/apisign/signing.html
