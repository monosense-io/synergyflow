# ADR-0007: OPA Sidecar for Policy Engine

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Backend Team

---

## Context

SynergyFlow requires policy-driven automation for:
- **Change approval routing**: Auto-approve low-risk changes, route high-risk to CAB
- **Incident assignment**: Route incidents based on skills, capacity, priority
- **Authorization decisions**: RBAC/ABAC for API endpoints
- **Explainable decisions**: Decision receipts showing "why" automation acted

Requirements:
- **Policy-as-code**: Rego policies versioned in Git
- **Low latency**: <100ms policy evaluation (p95 target <10ms)
- **Shadow mode**: Test policies without enforcing
- **Audit trail**: Log all policy decisions

Options:
1. **OPA sidecar** (deployed alongside Spring Boot pods)
2. **OPA cluster** (centralized OPA deployment)
3. **Custom policy engine** (Java-based rule engine)
4. **Cloud-managed** (AWS IAM, Azure Policy)

---

## Decision

**We will deploy OPA 0.68.0 as a sidecar container (1 per backend pod), NOT centralized cluster.**

### Rationale

**1. Ultra-Low Latency (<10ms)**
- **Localhost communication**: `http://localhost:8181/v1/data/...`
- **No network latency**: In-pod communication vs cross-pod network calls
- **Target achieved**: <10ms policy evaluation (vs 20-50ms for centralized OPA)

**2. Operational Simplicity**
```yaml
# Kubernetes Pod with sidecar
spec:
  containers:
  - name: backend
    image: synergyflow-backend:latest
  - name: opa
    image: openpolicyagent/opa:0.68.0
    args: ["run", "--server", "--log-level=info"]
    volumeMounts:
    - name: policies
      mountPath: /policies
  volumes:
  - name: policies
    configMap:
      name: opa-policies
```
- **No separate deployment**: OPA lifecycle managed with backend
- **Automatic scaling**: OPA scales with backend replicas (1:1 ratio)
- **Simplified debugging**: OPA logs alongside backend logs

**3. Explainable Decisions (Decision Receipts)**
```json
// OPA decision output
{
  "result": {
    "allow": false,
    "requiresCAB": true,
    "reason": "High-risk change impacting critical service",
    "policy_version": "v1.2.3"
  }
}
```
- **Human-readable**: "Why was this auto-approved/rejected?"
- **Audit trail**: Stored in `decision_receipts` table
- **Compliance**: SOC 2, ISO 27001 evidence

**4. Policy-as-Code (GitOps)**
- **Rego policies in Git**: `policies/change_approval_policy.rego`
- **CI/CD pipeline**: Policy tests run in CI, deployed as ConfigMap
- **Versioning**: Policy versions tracked, rollback supported
- **Shadow mode**: Test policies without enforcing (log-only mode)

**5. Resource Efficiency**
- **Lightweight**: 100m-500m CPU, 128-256Mi RAM per sidecar
- **Scales linearly**: 3 backend replicas = 3 OPA sidecars (acceptable overhead)

---

## Consequences

### Positive

✅ **Ultra-low latency**: <10ms policy evaluation (localhost communication)
✅ **Operational simplicity**: OPA lifecycle managed with backend, automatic scaling
✅ **Explainable decisions**: Decision receipts for audit trail, compliance
✅ **Policy-as-code**: Rego policies versioned in Git, CI/CD pipeline
✅ **Shadow mode**: Test policies before enforcement

### Negative

⚠️ **Resource overhead**: OPA sidecar per backend replica (100m CPU, 128Mi RAM × replicas)
⚠️ **Policy sync**: ConfigMap updates require pod restart (mitigated: bundle server for hot reload if needed)
⚠️ **Learning curve**: Team must learn Rego policy language

---

## Alternatives Considered

### Alternative 1: Centralized OPA Cluster

**Rejected because**: Network latency (20-50ms vs <10ms), separate deployment to manage, no scaling benefits.

### Alternative 2: Custom Java Rule Engine (Drools)

**Rejected because**: No policy-as-code, no shadow mode, complex API, less maintainable than Rego.

### Alternative 3: Cloud-Managed (AWS IAM, Azure Policy)

**Rejected because**: Vendor lock-in, self-hosted requirement, no decision receipts.

---

## References

- **OPA Documentation**: https://www.openpolicyagent.org/
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md)
- **Architecture**: [docs/architecture/11-backend-architecture.md](../architecture/11-backend-architecture.md)
