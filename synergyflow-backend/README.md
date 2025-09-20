# SynergyFlow Backend (Spring Modulith)

- Java 21, Gradle 8 (Kotlin DSL)
- Spring Boot 3.x (pinned to latest stable)
- Spring Modulith for module boundaries and testing
- Testcontainers, JUnit 5, AssertJ
- JaCoCo coverage gates

## Versions (at scaffold time)

- Spring Boot: 3.5.6
- Spring Modulith BOM: 1.4.3
- Testcontainers BOM: 1.21.3
- Lombok: 1.18.42
- MapStruct: 1.6.3

Update policy: track latest stable Boot 3.x; apply monthly patch bumps and minor upgrades after smoke tests.

## Tasks

- Build: `./gradlew build`
- Unit tests: `./gradlew test`
- Architecture tests: `./gradlew architectureTest`
- Integration tests: `./gradlew integrationTest`
- Coverage report: `./gradlew jacocoTestReport` (HTML in `build/reports/jacoco/test/html/index.html`)
- Run app: `./gradlew bootRun`

## Module Boundaries

All modules have been created with `package-info.java` containing `@ApplicationModule` annotations:
- `user` (with `user::api` named interface)
- `team` (depends only on `user::api` per ADR-0002)
- `incident`
- `problem`
- `change`
- `knowledge`
- `cmdb`
- `notification`
- `workflow`
- `integration`

Verify with:

```java
ApplicationModules.of(SynergyFlowApplication.class).verify();
```

## Test Support

Comprehensive test support utilities under `src/test/java/io/monosense/synergyflow/testsupport/**`:

- `containers/`: Reusable Testcontainers (PostgreSQL, Redis, Kafka) with automatic startup and reuse
- `events/`: Spring Modulith event testing helpers with fluent assertions
- `api/`: API client helpers for REST testing
- `time/`: Deterministic clock utilities for time-based testing

### Container Reuse Configuration

To enable container reuse for faster test execution, set the following environment variable:

```bash
export TESTCONTAINERS_REUSE_ENABLE=true
```

This allows containers to be reused across test runs, significantly reducing startup time.

## OAuth2 JWT Resource Server Configuration

The application is configured as an OAuth2 Resource Server that validates JWT tokens. It supports multiple Identity Provider formats including Keycloak, Auth0, and Okta.

### Required Environment Variables

```bash
# OAuth2 JWT Configuration
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://your-keycloak.example.com/realms/your-realm
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://your-keycloak.example.com/realms/your-realm/protocol/openid-connect/certs

# Optional JWT Configuration
SECURITY_JWT_EXPECTED_AUDIENCE=synergyflow-api
SECURITY_JWT_CLOCK_SKEW=60
# Optional CORS Configuration
SECURITY_CORS_ALLOWED_ORIGINS=https://partner.example.com,http://localhost:3001
```

### API Endpoints

#### Public Endpoints
- `GET /actuator/health` - Health check (no authentication required)
- `GET /actuator/info` - Application info (no authentication required)

#### Authenticated Endpoints
- `GET /api/whoami` - Returns current user information and roles (requires valid JWT)

#### Admin Endpoints
- `GET /api/admin/ping` - Admin-only endpoint (requires `ROLE_ADMIN`)

All authenticated responses echo the `X-Correlation-ID` header so distributed traces can be stitched across services. Provide this header on requests (or allow the backend to generate one) to maintain observability.

### Example API Usage

#### Get User Information
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/whoami
```

Expected response:
```json
{
  "subject": "user-id-123",
  "issuer": "https://your-keycloak.example.com/realms/your-realm",
  "audience": ["synergyflow-api"],
  "issuedAt": "2025-09-20T10:00:00Z",
  "expiresAt": "2025-09-20T11:00:00Z",
  "authorities": ["ROLE_USER", "ROLE_ADMIN"],
  "preferredUsername": "john.doe",
  "email": "john.doe@example.com",
  "name": "John Doe",
  "timestamp": "2025-09-20T10:30:00Z"
}
```

#### Admin Ping
```bash
curl -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
     http://localhost:8080/api/admin/ping
```

Expected response:
```json
{
  "message": "Admin ping successful",
  "adminUser": "admin-user-id",
  "timestamp": "2025-09-20T10:30:00Z"
}
```

### Supported Identity Provider Formats

#### Keycloak
Extracts roles from `realm_access.roles[]`:
```json
{
  "realm_access": {
    "roles": ["user", "admin"]
  }
}
```

#### Auth0/Okta
Extracts roles from `permissions[]` and `roles[]`:
```json
{
  "permissions": ["read:data", "write:data"],
  "roles": ["user"]
}
```

### Role Normalization

All roles are normalized to Spring Security format:
- Special characters (`:`, `-`, `.`) are replaced with underscores
- Converted to uppercase
- Prefixed with `ROLE_`

Examples:
- `read:user-data` → `ROLE_READ_USER_DATA`
- `admin` → `ROLE_ADMIN`

### MyBatis-Plus Integration

The application includes MyBatis-Plus3 for database access with the following configuration:
- Spring Boot 3 starter with JSQLParser support
- Camel case to snake_case mapping enabled
- Interceptors configured for optimistic locking, attack blocking, and pagination

## Next Steps

- Implement business logic in each module according to their domain responsibilities.
- Add more comprehensive event-driven integration tests as features are developed.
- Configure actual Identity Provider endpoints for production deployment.
