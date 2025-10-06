# Error Catalog — SynergyFlow APIs

Status: v1.0.0 (updated 2025-10-06)

## Standard Error Schema

All error responses follow this JSON schema:

```json
{
  "code": "string",           // Machine-readable error code (namespace/error_type)
  "message": "string",         // Human-readable error message
  "details": {                 // Optional context-specific details
    "field": "value",
    "constraint": "description"
  },
  "retryable": boolean,        // Whether client should retry (default: false)
  "retryAfter": number         // Seconds to wait before retry (optional)
}
```

## HTTP Status Code Mapping

| Status | Usage | Retry Strategy |
|--------|-------|----------------|
| 400 Bad Request | Validation errors, malformed input | ❌ No retry — fix request |
| 401 Unauthorized | Missing or invalid JWT token | ❌ No retry — re-authenticate |
| 403 Forbidden | Insufficient permissions for resource | ❌ No retry — request access |
| 404 Not Found | Resource ID does not exist | ❌ No retry — verify ID |
| 409 Conflict | Duplicate resource creation | ❌ No retry — use existing resource |
| 412 Precondition Failed | Version/ETag mismatch (optimistic locking) | ✅ Retry with GET → merge → PUT |
| 422 Unprocessable Entity | Semantically invalid (e.g., closed ticket cannot reopen) | ❌ No retry — invalid state transition |
| 429 Too Many Requests | Rate limit exceeded | ✅ Retry after `retryAfter` seconds |
| 500 Internal Server Error | Server-side failure | ✅ Retry with exponential backoff |
| 503 Service Unavailable | Temporary unavailability (maintenance, overload) | ✅ Retry after `retryAfter` seconds |

## Common Error Codes

### Authentication & Authorization
- `auth/unauthorized` (401) — Missing or invalid JWT bearer token
- `auth/forbidden` (403) — User lacks required role or scope for resource
- `auth/token_expired` (401) — JWT token expired, refresh required
- `auth/invalid_scope` (403) — Operation requires scope not granted to user

### Resource Errors
- `common/not_found` (404) — Resource not found by ID
- `common/already_exists` (409) — Duplicate resource creation attempt
- `common/version_mismatch` (412) — Optimistic locking failure (version/ETag mismatch)

### Validation Errors
- `common/bad_request` (400) — Request validation failure (field errors in `details`)
- `common/invalid_transition` (422) — State transition not allowed (e.g., "Closed → In Progress")
- `common/constraint_violation` (422) — Business rule violation (e.g., sprint over capacity)

### Rate Limiting
- `common/rate_limit_exceeded` (429) — Rate limit exceeded (1000 req/min per user)

### Service Errors
- `common/internal_error` (500) — Unhandled server-side error
- `common/service_unavailable` (503) — Service temporarily unavailable (maintenance or overload)
- `common/timeout` (504) — Upstream service timeout (>5s)

### Domain-Specific Errors (ITSM)
- `itsm/sla_already_breached` (422) — Cannot extend SLA after breach
- `itsm/invalid_assignment` (422) — Cannot assign ticket to user without role
- `itsm/routing_rule_conflict` (409) — Routing rule conflicts with existing rule

### Domain-Specific Errors (PM)
- `pm/sprint_locked` (422) — Cannot modify sprint after start date
- `pm/issue_in_sprint` (409) — Cannot delete issue assigned to active sprint
- `pm/invalid_board_column` (400) — Board column does not exist

### Domain-Specific Errors (Workflow)
- `workflow/approval_already_decided` (409) — Approval request already approved/rejected
- `workflow/policy_violation` (422) — Request violates approval policy
- `workflow/simulation_failed` (500) — Simulation engine error (budget/inventory adapter unavailable)

## Retry Guidance

### Retryable Errors (Exponential Backoff)
```
412 Precondition Failed → Retry immediately with GET → merge → PUT
429 Too Many Requests → Wait `retryAfter` seconds (typically 60s)
500 Internal Error → Exponential backoff: 1s, 2s, 4s, 8s, 16s (max 5 retries)
503 Service Unavailable → Wait `retryAfter` seconds or exponential backoff
504 Gateway Timeout → Exponential backoff (upstream service slow)
```

### Non-Retryable Errors
```
400, 401, 403, 404, 409, 422 → Client must fix request or accept failure
```

## Error Response Examples

### 400 Bad Request (Validation)
```json
{
  "code": "common/bad_request",
  "message": "Validation failed",
  "details": {
    "title": "Required field missing",
    "priority": "Invalid value 'URGENT' (allowed: LOW, MEDIUM, HIGH, CRITICAL)"
  },
  "retryable": false
}
```

### 401 Unauthorized
```json
{
  "code": "auth/unauthorized",
  "message": "Bearer token missing or invalid",
  "retryable": false
}
```

### 403 Forbidden
```json
{
  "code": "auth/forbidden",
  "message": "User lacks 'itsm:agent' role required for this operation",
  "details": {
    "requiredRole": "itsm:agent",
    "userRoles": ["employee"]
  },
  "retryable": false
}
```

### 404 Not Found
```json
{
  "code": "common/not_found",
  "message": "Ticket not found",
  "details": {
    "ticketId": "TICKET-12345"
  },
  "retryable": false
}
```

### 409 Conflict
```json
{
  "code": "workflow/approval_already_decided",
  "message": "Approval request already decided",
  "details": {
    "requestId": "req-uuid",
    "decision": "APPROVED",
    "decidedBy": "sarah@example.com",
    "decidedAt": "2025-10-05T14:30:00Z"
  },
  "retryable": false
}
```

### 412 Precondition Failed (Version Mismatch)
```json
{
  "code": "common/version_mismatch",
  "message": "Resource version mismatch",
  "details": {
    "expectedVersion": 7,
    "actualVersion": 6,
    "resolution": "GET latest version, merge changes, retry PUT with correct version"
  },
  "retryable": true
}
```

### 422 Unprocessable Entity (Invalid Transition)
```json
{
  "code": "common/invalid_transition",
  "message": "Invalid state transition",
  "details": {
    "currentStatus": "CLOSED",
    "requestedStatus": "IN_PROGRESS",
    "reason": "Closed tickets cannot be reopened"
  },
  "retryable": false
}
```

### 429 Too Many Requests
```json
{
  "code": "common/rate_limit_exceeded",
  "message": "Rate limit exceeded (1000 req/min)",
  "details": {
    "limit": 1000,
    "window": "60s",
    "resetAt": "2025-10-06T16:15:00Z"
  },
  "retryable": true,
  "retryAfter": 60
}
```

### 500 Internal Server Error
```json
{
  "code": "common/internal_error",
  "message": "An unexpected error occurred",
  "details": {
    "traceId": "abc123-def456-789",
    "contact": "support@synergyflow.io"
  },
  "retryable": true
}
```

### 503 Service Unavailable
```json
{
  "code": "common/service_unavailable",
  "message": "Service temporarily unavailable",
  "details": {
    "reason": "Scheduled maintenance",
    "estimatedRecovery": "2025-10-06T17:00:00Z"
  },
  "retryable": true,
  "retryAfter": 300
}
```

## Client Implementation Guide

### Retry Logic (Pseudocode)
```typescript
async function fetchWithRetry(url: string, options: RequestInit, maxRetries = 3) {
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetch(url, options);

      if (response.ok) return response;

      const error = await response.json();

      // Non-retryable errors
      if ([400, 401, 403, 404, 409, 422].includes(response.status)) {
        throw new ApiError(error);
      }

      // Retryable errors
      if ([412, 429, 500, 503, 504].includes(response.status)) {
        if (attempt === maxRetries) throw new ApiError(error);

        const delayMs = error.retryAfter
          ? error.retryAfter * 1000
          : Math.min(1000 * Math.pow(2, attempt), 16000); // Exponential backoff with 16s cap

        await sleep(delayMs);
        continue;
      }
    } catch (networkError) {
      // Network failures → retry with backoff
      if (attempt === maxRetries) throw networkError;
      await sleep(1000 * Math.pow(2, attempt));
    }
  }
}
```

### Optimistic Locking Pattern
```typescript
async function updateTicketWithRetry(ticketId: string, updates: Partial<Ticket>) {
  for (let attempt = 0; attempt < 3; attempt++) {
    // GET latest version
    const current = await getTicket(ticketId);

    // Merge changes
    const merged = { ...current, ...updates };

    // PUT with version
    try {
      return await updateTicket(ticketId, merged, {
        headers: { 'If-Match': current.version }
      });
    } catch (error) {
      if (error.code === 'common/version_mismatch' && attempt < 2) {
        continue; // Retry with fresh GET
      }
      throw error;
    }
  }
}
```

