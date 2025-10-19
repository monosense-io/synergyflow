# 7. External APIs

SynergyFlow integrates with the following external APIs:

## 7.1 OAuth2/OIDC Provider (Keycloak or Auth0)

**Purpose:** Authentication and JWT token issuance

**Documentation:**
- Keycloak: https://www.keycloak.org/docs/latest/securing_apps/
- Auth0: https://auth0.com/docs/api/authentication

**Base URL(s):**
- Keycloak: `https://keycloak.example.com/realms/synergyflow`
- Auth0: `https://synergyflow.auth0.com`

**Authentication:** OAuth2 Authorization Code Flow

**Rate Limits:** N/A (self-hosted Keycloak) or Auth0 plan limits

**Key Endpoints Used:**
- `GET /.well-known/openid-configuration` - OIDC discovery endpoint
- `POST /protocol/openid-connect/token` (Keycloak) or `/oauth/token` (Auth0) - Token issuance
- `GET /protocol/openid-connect/certs` (Keycloak) or `/.well-known/jwks.json` (Auth0) - JWT public keys for validation

**Integration Notes:**
- Frontend redirects to IdP for login (OAuth2 Authorization Code Flow)
- IdP issues JWT access token (1-hour expiration)
- Backend validates JWT signature using public key from JWKS endpoint
- Spring Security OAuth2 Resource Server handles validation automatically
- Envoy Gateway also validates JWT for additional security layer

## 7.2 1Password (via ExternalSecrets Operator)

**Purpose:** Secrets management (database credentials, API keys, JWT signing keys)

**Documentation:** https://developer.1password.com/docs/connect/

**Base URL(s):** `https://1password.example.com` (1Password Connect server)

**Authentication:** 1Password Connect token (stored as Kubernetes Secret)

**Rate Limits:** N/A (self-hosted Connect server)

**Key Endpoints Used:**
- `GET /v1/vaults/{vaultId}/items/{itemId}` - Fetch secret item

**Integration Notes:**
- ExternalSecrets Operator (ESO) deployed in cluster
- ESO polls 1Password Connect API every 5 minutes for secret updates
- Secrets synced to Kubernetes Secrets (referenced by Pods)
- Automatic rotation: ESO detects secret changes and updates Kubernetes Secrets
- Backend reads secrets from environment variables (no plaintext in Git)

---
