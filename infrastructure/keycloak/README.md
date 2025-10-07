# Keycloak Configuration for SynergyFlow

This directory contains Keycloak realm configuration for the SynergyFlow application.

## Realm Configuration

The `synergyflow-realm.json` file contains the complete realm configuration including:

- **Realm**: `synergyflow`
- **Client**: `synergyflow-backend` (confidential client for OAuth2 Resource Server)
- **Roles**: `admin`, `itsm_agent`, `developer`, `manager`, `employee`
- **Test Users**:
  - `admin@test.com` (password: `admin123`) - Admin role
  - `agent1@test.com` (password: `agent123`) - ITSM Agent role
  - `dev1@test.com` (password: `dev123`) - Developer role

## Setup Instructions

### Option 1: Import Realm Configuration (Recommended)

1. Start Keycloak (via Docker Compose or standalone):
   ```bash
   # If using Docker Compose
   docker-compose up -d keycloak
   ```

2. Access Keycloak Admin Console:
   - URL: http://localhost:8080
   - Username: `admin`
   - Password: `admin`

3. Import the realm:
   - Click on the realm dropdown (top-left, currently showing "master")
   - Click "Create Realm"
   - Click "Browse" and select `infrastructure/keycloak/synergyflow-realm.json`
   - Click "Create"

4. The realm will be imported with all roles, clients, and test users configured.

### Option 2: Manual Configuration

If you prefer to configure manually:

1. **Create Realm**:
   - Click on the realm dropdown → "Create Realm"
   - Realm name: `synergyflow`
   - Click "Create"

2. **Create Client**:
   - Navigate to Clients → "Create client"
   - Client ID: `synergyflow-backend`
   - Client Protocol: `openid-connect`
   - Click "Next"
   - Client authentication: ON (confidential)
   - Authorization: OFF
   - Standard flow: ON
   - Direct access grants: ON
   - Service accounts: ON
   - Click "Save"
   - Go to "Credentials" tab → Copy the client secret
   - Go to "Settings" tab:
     - Valid Redirect URIs: `http://localhost:8081/*`, `http://localhost:5173/*`
     - Web Origins: `http://localhost:5173`, `http://localhost:8081`

3. **Create Realm Roles**:
   - Navigate to Realm roles → "Create role"
   - Create the following roles:
     - `admin` - Administrator with full system access
     - `itsm_agent` - ITSM Agent for ticket operations
     - `developer` - Developer for PM operations
     - `manager` - Manager for approval workflows
     - `employee` - Employee for self-service requests

4. **Create Test Users**:
   - Navigate to Users → "Add user"
   - Create users and assign roles:
     - `admin@test.com` with role `admin`
     - `agent1@test.com` with role `itsm_agent`
     - `dev1@test.com` with role `developer`
   - Set passwords: Users → Credentials → Set password

## Environment Variables

Configure the following environment variables in your application:

```bash
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/synergyflow
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/synergyflow/protocol/openid-connect/certs
```

These are already configured with defaults in `backend/src/main/resources/application.yml`.

## Testing Authentication

### Obtain Access Token

```bash
curl -X POST http://localhost:8080/realms/synergyflow/protocol/openid-connect/token \
  -d "client_id=synergyflow-backend" \
  -d "client_secret=synergyflow-secret-change-in-production" \
  -d "grant_type=password" \
  -d "username=admin@test.com" \
  -d "password=admin123" | jq -r .access_token
```

### Test Secured Endpoint

```bash
# Save token to variable
TOKEN=$(curl -s -X POST http://localhost:8080/realms/synergyflow/protocol/openid-connect/token \
  -d "client_id=synergyflow-backend" \
  -d "client_secret=synergyflow-secret-change-in-production" \
  -d "grant_type=password" \
  -d "username=admin@test.com" \
  -d "password=admin123" | jq -r .access_token)

# Call secured endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Production Considerations

**⚠️ IMPORTANT**: Before deploying to production:

1. **Change Client Secret**: Update the client secret in Keycloak Admin Console
2. **Use HTTPS**: Configure Keycloak with SSL/TLS certificates
3. **Update Redirect URIs**: Add production domain URIs
4. **Remove Test Users**: Delete or disable test users
5. **Enable MFA**: Consider enabling multi-factor authentication
6. **Password Policy**: Configure strong password policies
7. **Session Timeouts**: Adjust token lifespans based on security requirements

## Troubleshooting

### JWT Validation Fails

- Verify Keycloak is running and accessible at configured issuer URI
- Check that the JWK Set URI is correct and returns public keys
- Ensure token hasn't expired (default: 1 hour)
- Verify client ID and secret match configuration

### Roles Not Mapped

- Ensure user has roles assigned in Keycloak
- Check that roles are realm roles (not client roles)
- Verify `realm_access.roles` claim is present in JWT token
- Check RoleMapper service logs for warnings about unknown roles
