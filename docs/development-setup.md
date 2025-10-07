# Development Setup Guide

This guide summarizes the minimum steps required to spin up the SynergyFlow development stack with secured Keycloak authentication (Story 1.4).

## Prerequisites

- Docker Desktop (or compatible container runtime)
- Java 21 (Temurin recommended)
- Node.js 20 LTS
- `jq` CLI for parsing JSON responses (optional but used in examples)

## 1. Start Supporting Services

```bash
cd infrastructure
# PostgreSQL, Redis, Keycloak, supporting services
docker-compose up -d
```

Verify that Keycloak is healthy:

```bash
curl -f http://localhost:8080/health
```

## 2. Import Keycloak Realm

The repository provides an export at `infrastructure/keycloak/synergyflow-realm.json` with:
- Realm: `synergyflow`
- Client: `synergyflow-backend` (confidential)
- Realm roles: `admin`, `itsm_agent`, `developer`, `manager`, `employee`
- Test users:
  - `admin@test.com` / `admin123`
  - `agent1@test.com` / `agent123`
  - `dev1@test.com` / `dev123`

Import steps (Keycloak Admin Console → **Add realm** → Upload JSON) are documented in `infrastructure/keycloak/README.md`.

## 3. Backend Configuration

Environment variables are optional because defaults are provided in `backend/src/main/resources/application.yml`, but the backend supports overriding at runtime:

```bash
export KEYCLOAK_ISSUER_URI=${KEYCLOAK_ISSUER_URI:-http://localhost:8080/realms/synergyflow}
export KEYCLOAK_JWK_SET_URI=${KEYCLOAK_JWK_SET_URI:-http://localhost:8080/realms/synergyflow/protocol/openid-connect/certs}
```

Run the backend:

```bash
cd backend
./gradlew bootRun
```

The security filter chain requires valid Bearer tokens for `/actuator/health` and any future API endpoints.

## 4. Obtain Test Token & Call Secured Endpoint

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/synergyflow/protocol/openid-connect/token \
  -d "client_id=synergyflow-backend" \
  -d "client_secret=synergyflow-secret-change-in-production" \
  -d "grant_type=password" \
  -d "username=admin@test.com" \
  -d "password=admin123" | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

Without a token (or with an invalid token) the endpoint returns `401 Unauthorized`.

## 5. Frontend Configuration (Preview)

When the React frontend consumes the backend, configure the OAuth client:

- `VITE_AUTH_ISSUER=http://localhost:8080/realms/synergyflow`
- `VITE_AUTH_CLIENT_ID=synergyflow-frontend`
- Configure PKCE/implicit flow once the frontend story reaches authentication.

## 6. Troubleshooting

| Symptom | Resolution |
| --- | --- |
| `401 Unauthorized` on `/actuator/health` | Ensure Bearer token present, `iss` matches `KEYCLOAK_ISSUER_URI`, and token not expired. |
| Backend cannot fetch JWKS | Verify Keycloak running and `KEYCLOAK_JWK_SET_URI` reachable. |
| Roles not mapped | Confirm user has realm roles (not client roles) and review backend logs for `Unknown Keycloak role` warnings. |

## References

- `backend/src/main/java/io/monosense/synergyflow/security/config/SecurityConfig.java`
- `backend/src/main/java/io/monosense/synergyflow/security/api/JwtValidator.java`
- `backend/src/main/java/io/monosense/synergyflow/security/api/RoleMapper.java`
- `backend/src/test/java/io/monosense/synergyflow/security/SecurityIntegrationTest.java`
- `infrastructure/keycloak/README.md`
