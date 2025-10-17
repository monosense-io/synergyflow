# Database Backup and Restore Procedures

**Date**: 2025-10-17
**Purpose**: Operational procedures for backing up and restoring SynergyFlow PostgreSQL database
**Audience**: DevOps, Operations, On-call Engineers
**Critical**: YES - Must be tested before MVP deployment

---

## Executive Summary

SynergyFlow uses CloudNative-PG with automated daily backups to S3. This document covers:
- Automated backup procedures (already configured)
- Manual backup procedures (emergency backups)
- Restore procedures (Point-in-Time Recovery and full restore)
- Backup verification (testing recovery)
- Troubleshooting (common issues)

**RTO/RPO Targets**:
- **RTO** (Recovery Time Objective): < 1 hour
- **RPO** (Recovery Point Objective): < 1 hour (hourly backups)

---

## Part 1: Automated Backup Configuration

### Current Setup

**CloudNative-PG Backup Schedule**:
```yaml
# In shared-postgres cluster (cnpg-system namespace)
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: shared-postgres
  namespace: cnpg-system
spec:
  backup:
    # Daily backup at 02:00 UTC
    barmanObjectStore:
      destinationPath: s3://synergyflow-backups/postgres
      s3Credentials:
        accessKeyId:
          name: aws-backup-credentials
          key: AWS_ACCESS_KEY_ID
        secretAccessKey:
          name: aws-backup-credentials
          key: AWS_SECRET_ACCESS_KEY
      data:
        compression: gzip
        maxParallel: 4
        retention: 30d           # 30-day hot storage
      wal:
        compression: gzip
        maxParallel: 2
        archiveTimeout: 5m
```

**Backup Details**:
- **Frequency**: Daily at 02:00 UTC
- **Type**: Full backup + WAL archiving
- **Compression**: gzip (reduces storage by 70%)
- **Retention**: 30 days hot (S3 standard), 7 years cold (S3 Glacier)
- **Destination**: `s3://synergyflow-backups/postgres/`
- **Scope**: Entire shared-postgres cluster (all databases including synergyflow)

### Automated Backups - No Action Needed

✅ Backups are automatically created daily
✅ WAL files archived every 5 minutes
✅ S3 bucket versioning enabled
✅ Cross-region replication configured

**Monitoring** (CloudNative-PG automatically tracks):
```bash
# Check backup status
kubectl get backup -n cnpg-system
kubectl describe backup -n cnpg-system

# Check WAL archiving status
kubectl logs -n cnpg-system -l cnpg.io/cluster=shared-postgres | grep barman
```

---

## Part 2: Manual Backup Procedures

### When to Use Manual Backups

- ⚠️ **Before major schema changes** (migrations, index creation)
- ⚠️ **Before production deployments** (as extra safety net)
- ⚠️ **Emergency situation** (data corruption, hacking incident)
- ⚠️ **Compliance requirement** (audit trail, proof of backup)

### Procedure: Create On-Demand Backup

**Step 1: Trigger Manual Backup** (5 minutes)
```bash
# Create backup resource in Kubernetes
kubectl apply -f - <<EOF
apiVersion: postgresql.cnpg.io/v1
kind: Backup
metadata:
  name: synergyflow-backup-$(date +%Y%m%d-%H%M%S)
  namespace: cnpg-system
spec:
  cluster:
    name: shared-postgres
  target: primary
  method: barmanObjectStore
EOF

# Verify backup started
kubectl get backup -n cnpg-system -w
# Wait for STATUS: "completed"
```

**Step 2: Verify Backup in S3** (2 minutes)
```bash
# List backups
aws s3 ls s3://synergyflow-backups/postgres/base/ --recursive | tail -20

# Check backup size
aws s3 ls s3://synergyflow-backups/postgres/base/ --recursive --summarize | tail -5
# Should show: Total Size: ~X GB (compressed)
```

**Step 3: Log Backup** (Documentation)
```bash
# Record in backup log
echo "$(date): Manual backup created before <reason>" >> /var/log/synergyflow/backup.log

# Alert team
# Send Slack message: "Manual backup completed: synergyflow-backup-20251017-120000"
```

---

## Part 3: Point-in-Time Recovery (PITR)

### How PITR Works

SynergyFlow keeps:
- Daily full backups (hot, 30 days)
- WAL archives (hot, 30 days; cold, 7 years)

This allows recovery to **any point in time within last 30 days**.

### Procedure: Restore to Point in Time

**Use Case**: Data corruption detected at 14:30 UTC, want to restore database to 14:00 UTC

**Step 1: Identify Recovery Target** (5 minutes)
```bash
# Option A: Specific timestamp
RECOVERY_TARGET_TIME="2025-10-17 14:00:00+00"

# Option B: Transaction ID (if available)
# RECOVERY_TARGET_XID="123456789"

# Option C: LSN (Log Sequence Number, if available)
# RECOVERY_TARGET_LSN="0/12345678"

echo "Recovering to: $RECOVERY_TARGET_TIME"
```

**Step 2: Create Recovery Cluster** (10 minutes)

```bash
# Create temporary recovery cluster
kubectl apply -f - <<EOF
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: synergyflow-recovery
  namespace: cnpg-system
spec:
  description: "Temporary recovery cluster for PITR"
  imageName: ghcr.io/cloudnative-pg/postgresql:16.8
  instances: 1

  # Storage for recovery cluster
  storage:
    size: 100Gi
    storageClass: rook-ceph-block

  # Recovery configuration
  recovery:
    backup:
      name: <last-completed-backup>  # From kubectl get backup
    recoveryTarget:
      targetTime: "2025-10-17T14:00:00Z"
      inclusive: true  # Include the target time in recovery

  # WAL archiving source
  barmanObjectStore:
    destinationPath: s3://synergyflow-backups/postgres
    s3Credentials:
      accessKeyId:
        name: aws-backup-credentials
        key: AWS_ACCESS_KEY_ID
      secretAccessKey:
        name: aws-backup-credentials
        key: AWS_SECRET_ACCESS_KEY
EOF

# Monitor recovery
kubectl get cluster synergyflow-recovery -n cnpg-system -w
# Wait for status: "Cluster in healthy state"
```

**Step 3: Verify Recovered Data** (15 minutes)

```bash
# Port-forward to recovery cluster
kubectl port-forward -n cnpg-system pod/synergyflow-recovery-1 5432:5432 &

# Connect to recovery database
psql -h localhost -U synergyflow_app synergyflow <<EOF
-- Check data state at recovery time
SELECT COUNT(*) as incident_count FROM synergyflow_incidents.incidents;
SELECT MAX(created_at) as latest_incident FROM synergyflow_incidents.incidents;
SELECT COUNT(*) as event_count FROM event_publication WHERE completion_date IS NULL;

-- Verify business logic
SELECT COUNT(DISTINCT incident_id) as open_incidents
FROM synergyflow_incidents.incidents
WHERE status NOT IN ('CLOSED', 'RESOLVED');
EOF

# If data looks correct: proceed to Step 4
# If data looks wrong: try different recovery time and repeat
```

**Step 4: Backup Recovery Cluster** (10 minutes)

```bash
# Create full backup of recovered data
kubectl apply -f - <<EOF
apiVersion: postgresql.cnpg.io/v1
kind: Backup
metadata:
  name: synergyflow-recovery-verified-$(date +%Y%m%d-%H%M%S)
  namespace: cnpg-system
spec:
  cluster:
    name: synergyflow-recovery
  target: primary
  method: barmanObjectStore
EOF

# Wait for backup to complete
kubectl get backup synergyflow-recovery-verified-* -n cnpg-system -w
```

**Step 5: Promote Recovery Cluster (DESTRUCTIVE)** (30 minutes)

⚠️ **WARNING**: This step overwrites production data. Only proceed if:
- Data verification passed (Step 3)
- Manual backup of production cluster completed (Part 2)
- All stakeholders approved

```bash
# Procedure:
# 1. Scale down SynergyFlow application (prevent writes)
kubectl scale deployment synergyflow-backend -n synergyflow --replicas=0

# 2. Wait 30 seconds for graceful shutdown
sleep 30

# 3. Delete old production cluster (CANNOT BE UNDONE)
kubectl delete cluster shared-postgres -n cnpg-system --grace-period=300

# 4. Rename recovery cluster to production
kubectl patch cluster synergyflow-recovery -n cnpg-system -p \
  '{"metadata":{"name":"shared-postgres"}}' --type merge

# 5. Update connection strings (if changed)
# Usually not needed if using cluster DNS (shared-postgres-rw.cnpg-system)

# 6. Wait for cluster to stabilize
kubectl get cluster shared-postgres -n cnpg-system -w

# 7. Restore SynergyFlow application
kubectl scale deployment synergyflow-backend -n synergyflow --replicas=3
kubectl rollout status deployment/synergyflow-backend -n synergyflow

# 8. Verify application
curl https://api.synergyflow.io/api/v1/health
```

**Step 6: Post-Recovery Validation** (30 minutes)

```bash
# 1. Check application logs (no errors)
kubectl logs -n synergyflow deployment/synergyflow-backend --tail=100 | grep -i error

# 2. Run smoke tests
# Test incident creation
curl -X POST https://api.synergyflow.io/api/v1/incidents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Incident","description":"PITR test","priority":"HIGH"}'

# Test data consistency
psql -h shared-postgres-rw.cnpg-system.svc.cluster.local \
  -U synergyflow_app synergyflow -c "SELECT COUNT(*) as incidents FROM synergyflow_incidents.incidents;"

# 3. Monitor for errors (1-2 hours)
# - Application errors: should be 0
# - Database latency: should be normal
# - Backup status: should show successful auto-backup

# 4. Send recovery notification
# "PITR completed successfully. Database recovered to 2025-10-17 14:00:00 UTC"
```

---

## Part 4: Full Database Restore

### Use Case
Disaster recovery scenario: entire database cluster corrupted or lost

### Procedure: Full Restore

**Step 1: Prepare Recovery Environment** (10 minutes)

```bash
# Ensure S3 backups accessible
aws s3 ls s3://synergyflow-backups/postgres/base/ | head -1

# Note latest backup information
LATEST_BACKUP=$(aws s3 ls s3://synergyflow-backups/postgres/base/ --recursive | tail -1 | awk '{print $NF}')
echo "Latest backup: $LATEST_BACKUP"
```

**Step 2: Create Recovery Cluster from Latest Backup** (15 minutes)

```bash
# Get last backup reference
kubectl get backup -n cnpg-system --sort-by=.metadata.creationTimestamp | tail -1

# Create new cluster from that backup
kubectl apply -f - <<EOF
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: synergyflow-full-recovery
  namespace: cnpg-system
spec:
  description: "Full recovery cluster from latest backup"
  imageName: ghcr.io/cloudnative-pg/postgresql:16.8
  instances: 3

  storage:
    size: 100Gi
    storageClass: rook-ceph-block

  # Use latest backup
  bootstrap:
    recovery:
      backup:
        name: <latest-backup-name>  # Get from step above

  # Continue WAL recovery
  barmanObjectStore:
    destinationPath: s3://synergyflow-backups/postgres
    s3Credentials:
      accessKeyId:
        name: aws-backup-credentials
        key: AWS_ACCESS_KEY_ID
      secretAccessKey:
        name: aws-backup-credentials
        key: AWS_SECRET_ACCESS_KEY
EOF

# Monitor recovery progress
kubectl get cluster synergyflow-full-recovery -n cnpg-system -w
# Watch for: replicas becoming "Cluster in healthy state"
# Typical time: 15-30 minutes depending on backup size
```

**Step 3: Verify Restored Data** (15 minutes)

```bash
# Connect to recovered cluster
POSTGRES_POD="synergyflow-full-recovery-1"

kubectl exec -it -n cnpg-system $POSTGRES_POD -- psql -U postgres synergyflow <<EOF
-- Verify all modules present
\dn  -- List all schemas

-- Check data integrity
SELECT COUNT(*) as tables FROM information_schema.tables
WHERE table_schema LIKE 'synergyflow_%';

-- Verify recent data
SELECT COUNT(*) as incidents FROM synergyflow_incidents.incidents;
SELECT COUNT(*) as changes FROM synergyflow_changes.changes;
SELECT COUNT(*) as tasks FROM synergyflow_tasks.tasks;
SELECT MAX(created_at) as last_modification FROM (
  SELECT created_at FROM synergyflow_incidents.incidents
  UNION ALL
  SELECT created_at FROM synergyflow_changes.changes
) recent;

-- Check event publication table
SELECT COUNT(*) as pending_events FROM event_publication WHERE completion_date IS NULL;
EOF
```

**Step 4: Promote Recovery Cluster** (30 minutes)

⚠️ **DESTRUCTIVE OPERATION** - see Part 3, Step 5 for details

```bash
# Scale down application
kubectl scale deployment synergyflow-backend -n synergyflow --replicas=0
sleep 30

# Delete corrupted cluster
kubectl delete cluster shared-postgres -n cnpg-system --grace-period=300

# Promote recovery cluster
kubectl patch cluster synergyflow-full-recovery -n cnpg-system \
  -p '{"metadata":{"name":"shared-postgres"}}' --type merge

# Wait for stability
kubectl get cluster shared-postgres -n cnpg-system -w

# Restore application
kubectl scale deployment synergyflow-backend -n synergyflow --replicas=3
kubectl rollout status deployment/synergyflow-backend -n synergyflow
```

**Step 5: Post-Recovery Validation**

Same as Part 3, Step 6 (see above)

---

## Part 5: Backup Verification Testing

### Why Test Backups?

"Backups that haven't been tested might as well not exist"

- 30% of backup failures go unnoticed until actual recovery needed
- Test your RTO/RPO annually
- Build confidence in recovery procedures

### Procedure: Monthly Backup Verification

**Schedule**: First Sunday of each month, 10:00 UTC

**Duration**: 2-3 hours

**Team**: On-call engineer + backup

**Step 1: Pre-test Checklist** (15 minutes)

```bash
# Verify no active incidents
# Check team availability (2 people minimum)
# Notify stakeholders: "Testing backup recovery"

# Verify latest backup exists and is recent
aws s3 ls s3://synergyflow-backups/postgres/base/ --recursive | tail -1
# Should show: within last 24 hours

# Verify WAL archiving active
kubectl logs -n cnpg-system -l cnpg.io/cluster=shared-postgres | grep barman | tail -5
# Should show: "WAL received" messages
```

**Step 2: Simulate Data Corruption** (5 minutes)

```bash
# In test environment, intentionally corrupt a small table
# DO NOT DO THIS IN PRODUCTION

# Create test database
createdb -h localhost synergyflow_test -U postgres

# Copy production schema only (no data)
pg_dump -h shared-postgres-rw.cnpg-system.svc.cluster.local \
  -U postgres synergyflow --schema-only | \
  psql -h localhost synergyflow_test -U postgres

# Restore from backup to verify it works
# (This is what we're testing)
```

**Step 3: Perform PITR Recovery** (45 minutes)

```bash
# Use procedures from Part 3 above
# Target: 1 hour ago (or last significant backup)
# Verify: Data is present and correct
# Measure: Time to complete recovery (should be < 45 minutes)
```

**Step 4: Record Results** (10 minutes)

```bash
# Document in backup testing log
cat >> /var/log/synergyflow/backup-tests.log <<EOF
Date: $(date)
Test Type: Monthly PITR Test
Recovery Time: XX minutes
Data Verification: PASSED/FAILED
Issues Found: None / [list any issues]
Next Test: $(date -d '+1 month')
EOF

# Send report to team
# "Monthly backup test completed successfully"
# "RTO verified: XX minutes"
# "All data verified present and correct"
```

---

## Part 6: Troubleshooting

### Issue: Backup Failed

**Symptoms**:
```
kubectl get backup -n cnpg-system
# Status: Failed or Pending for >2 hours
```

**Diagnosis**:
```bash
# Check backup logs
kubectl describe backup <backup-name> -n cnpg-system
kubectl logs -n cnpg-system -l cnpg.io/cluster=shared-postgres | grep -i error

# Check S3 connectivity
aws s3 ls s3://synergyflow-backups/

# Check credentials
kubectl get secret aws-backup-credentials -n cnpg-system -o yaml
```

**Resolution**:
```bash
# 1. Verify S3 bucket permissions
# 2. Check AWS credentials are valid
# 3. Verify network connectivity to S3
# 4. Delete failed backup resource
kubectl delete backup <failed-backup> -n cnpg-system

# 5. Manually trigger new backup
kubectl apply -f - <<EOF
apiVersion: postgresql.cnpg.io/v1
kind: Backup
metadata:
  name: manual-retry-$(date +%s)
  namespace: cnpg-system
spec:
  cluster:
    name: shared-postgres
  target: primary
  method: barmanObjectStore
EOF
```

### Issue: Recovery Cluster Slow

**Symptoms**:
- Recovery taking > 1 hour
- PostgreSQL not becoming ready
- CPU/memory spike

**Diagnosis**:
```bash
# Check recovery progress
kubectl logs -n cnpg-system <recovery-pod> | grep -i recovery

# Check storage I/O
kubectl top pod <recovery-pod> -n cnpg-system

# Check WAL replay progress
kubectl exec <recovery-pod> -n cnpg-system -- \
  tail -100 /var/log/postgresql/postgresql.log | grep -i replay
```

**Resolution**:
```bash
# Increase recovery cluster resources
kubectl patch cluster synergyflow-recovery -n cnpg-system \
  --type merge -p \
  '{"spec":{"resources":{"requests":{"cpu":"4","memory":"8Gi"}}}}'

# Or provision faster storage class
kubectl patch cluster synergyflow-recovery -n cnpg-system \
  --type merge -p \
  '{"spec":{"storage":{"storageClass":"fast-ssd"}}}'
```

### Issue: Connection String Changed After Recovery

**Symptoms**:
- Application can't connect after recovery
- Errors: "could not resolve synergyflow-recovery-rw.cnpg-system.svc.cluster.local"

**Resolution**:
```bash
# DNS name should be: shared-postgres-rw.cnpg-system.svc.cluster.local
# (same as before recovery)

# If recovery cluster has different name:
# 1. Patch cluster name
kubectl patch cluster synergyflow-recovery \
  --type merge -p '{"metadata":{"name":"shared-postgres"}}'

# 2. Or update application connection string
kubectl set env deployment/synergyflow-backend \
  SPRING_DATASOURCE_URL="jdbc:postgresql://synergyflow-recovery-rw.cnpg-system.svc.cluster.local:5432/synergyflow"
```

---

## Part 7: Operational Procedures

### Daily Operations (Automated - No Action)

✅ 02:00 UTC: Daily backup created
✅ Every 5 minutes: WAL files archived to S3
✅ Hourly: Backup status checked

**Verification** (run weekly):
```bash
# Verify latest backup completed successfully
kubectl get backup -n cnpg-system --sort-by=.metadata.creationTimestamp | tail -1
# Should show "Completed" status within last 24 hours
```

### Monthly Procedures

📋 First Sunday of month: Backup verification test (Part 5)
📋 Review backup metrics and logs

### Quarterly Procedures

📋 Full restore drill to new environment
📋 Update RTO/RPO targets based on observed performance

### Annual Procedures

📋 Long-term backup restoration test (from archive/Glacier)
📋 Review and update backup procedures
📋 Audit backup retention policies

---

## Part 8: Checklist Before MVP Deployment

- [ ] Automated backups configured and running
- [ ] Daily backup status verified (3 consecutive days)
- [ ] Manual backup procedure tested (create and verify)
- [ ] PITR procedure tested in dev environment
- [ ] Full restore procedure documented and tested
- [ ] Recovery times measured (should be < 1 hour)
- [ ] RTO/RPO confirmed and documented
- [ ] Team trained on backup procedures
- [ ] On-call runbook updated with recovery procedures
- [ ] Monthly backup testing scheduled

---

## Summary

| Procedure | Frequency | Time | Purpose |
|-----------|-----------|------|---------|
| Automated backup | Daily 02:00 UTC | 30 min | Primary backup |
| WAL archiving | Every 5 min | <1 min | PITR capability |
| Manual backup | On-demand | 10 min | Pre-deployment safety |
| PITR recovery | Emergency only | 1 hour | Point-in-time restore |
| Full restore | Emergency only | 1 hour | Complete recovery |
| Backup verification | Monthly | 2-3 hours | Test recovery procedures |
| Long-term restore | Annual | 4-6 hours | Archive recovery test |

---

**Document Status**: ✅ Complete - Ready for Operations team
**Last Updated**: 2025-10-17
**Next Review**: After first successful PITR test

