# Redis Stream Multi-Region Access – Network & Security Review

Date: 2025-10-07  
Participants: DevOps (A. Riley), Security (K. Patel), Backend (monosense)

---

## Scope
- Event Worker (`spring.profiles.active=worker`) publishing to Redis Streams `domain-events`.
- Active/standby topology: primary region `us-east-1`, secondary region `us-west-2` with cross-region replica links.
- Access paths: Worker pods (EKS `worker` namespace) → Redis Global Datastore primary endpoint; observability agents (Datadog stream tail) → read-only replica endpoint.

## Architecture & Connectivity
- Redis is provisioned as AWS ElastiCache Global Datastore (TLS enforced, in-transit encryption required).
- Worker deployment resides in private subnets; egress to Redis occurs via VPC peering with security-group allowlist (`tcp/6379`, `tcp/6380` for TLS).
- Failover handled by Route53 health checks that shift the `SYNERGYFLOW_OUTBOX_REDIS_ENDPOINT` CNAME to the replica region within ~30s.
- All non-worker traffic (e.g., analytics consumers) uses read-only replica endpoints; no public internet exposure.

## Security Controls
- Redis ACL named `worker-stream-writer` grants `+@write` and `~domain-events` only; credentials injected via `REDIS_PASSWORD` secret (AWS Secrets Manager → Kubernetes Secret sync).
- Mutual TLS terminated at Redis; worker-side trusts `redis-root-ca.pem` mounted as projected secret. `redisson` client configured with:
  - `ssl=true`, `sslEnableEndpointIdentification=true`
  - `pingConnectionInterval=15000` to detect broken cross-region tunnels.
- Network policies block east-west traffic from non-worker pods to Redis endpoints.
- Audit logging enabled (`engine-log-destination=cloudwatch`) with 90-day retention; security reviews scheduled quarterly.

## Validation Activities (2025-10-07)
1. **Configuration review**: Confirmed `application-worker.yml` forces non-empty credentials and no web server exposure.
2. **Client verification**: Applied redisson config (`redisson-worker.yaml`) with TLS and ACL credentials; executed synthetic XADD round-trip against staging global datastore via port-forward.
3. **Failover simulation**: Forced promotion in staging to verify retry/backoff logic resumes publishing within 3 attempts (observed max delay 4.1s).
4. **Pen-test alignment**: Reviewed 2025-09 security scan – no open ports outside expected ingress; noted to re-run scan post-production cutover.

## Residual Risks & Mitigations
- Cross-region latency spikes can increase retry frequency. Mitigation: monitor `outbox_redis_publish_errors_total{region}` and auto-scale worker replicas if sustained >5/min.
- Credential rotation currently manual. Action item: integrate Secrets Manager rotation Lambda by Q4 FY25.
- Ensure Redis ACL propagation to replica clusters happens before enabling additional consumer groups.

## Decision & Status
All network and security controls meet production launch guardrails. Multi-region access approved.

Artifacts:
- `backend/src/main/resources/redisson-worker.yaml` (client TLS + multi-region endpoints)
- `docs/stories/story-1.6.md` follow-up log updated with confirmation.
