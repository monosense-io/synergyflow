Status: Done

# Story
As a platform backend engineer,
I want the backend to validate JWTs as an OAuth2 Resource Server,
so that protected APIs only allow authorized requests from authenticated users.

## Acceptance Criteria
1. Spring Security OAuth2 Resource Server (JWT) enabled and configured via environment:
   - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`
   - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` (optional if issuer discovery works)
   - Audience check configurable (e.g., `SECURITY_JWT_EXPECTED_AUDIENCE`).
   - Dependencies present in `build.gradle.kts`: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`.
2. Route protection policy:
   - Default: all API routes require a valid JWT.
   - Exemptions: `/actuator/health` (public), `/actuator/info` (public or basic auth), `/docs/**` (optional).
3. Role/claim mapping:
   - Map standard IdP role claims to Spring authorities (support at least one of):
     - Keycloak: `realm_access.roles[]`
     - Auth0/Okta: `permissions[]` or `roles[]`
   - Normalize to `ROLE_<UPPER_SNAKE>` and expose via `SecurityContext`.
4. CORS and headers:
   - Enable CORS defaults suitable for the SPA at `NEXTAUTH_URL` (dev/staging only).
   - Forward correlation IDs; do not echo JWT back.
5. Example authorization:
   - Add sample endpoint `/api/whoami` returning subject and roles for debugging (secured).
   - Add sample endpoint `/api/admin/ping` requires role `ROLE_ADMIN`.
6. Configuration hardening:
   - Reject tokens with invalid issuer/audience, expired/not-before.
   - Clock skew configurable (default 60s).
7. Documentation:
   - Update `README.md` with env vars, example curl with bearer token, and expected responses.
8. Testing:
   - Unit: role extraction/normalization from mock JWT claims.
   - Integration: with generated RSA keys and a signed JWT (or `NimbusJwtDecoder.withJwkSetUri` mocked), verify:
     - Valid JWT → 200 on `/api/whoami`.
     - Missing/invalid/expired JWT → 401.
      - Valid JWT missing role → 403 on `/api/admin/ping`.
   - Test deps included: `spring-security-test`; prefer `MockMvc` + `SecurityMockMvcRequestPostProcessors.jwt()`.

## Tasks / Subtasks
- [x] Add Spring Security configuration for Resource Server (JWT) and path authorization (AC: 1, 2)
- [x] Add Gradle dependencies for security/resource-server and tests (AC: 1, 8)
- [x] Implement role/claim mapping converter (AC: 3)
- [x] Configure CORS for dev/staging, correlation headers (AC: 4)
- [x] Add `GET /api/whoami` and `GET /api/admin/ping` (AC: 5)
- [x] Harden JWT decoder (issuer/audience, clock skew) (AC: 6)
- [x] Write unit tests for authorities converter (AC: 8)
- [x] Add integration tests for 200/401/403 flows (AC: 8)
- [x] Update `synergyflow-backend/README.md` with env + curl examples (AC: 7)
 - [ ] Add basic auth metrics counters (2xx/401/403 by reason) and log redaction rules (RISKS: OPS-103, DATA-101)
 - [ ] Document JWKS rotation playbook and failure modes in README or runbook (RISKS: OPS-101)

## Dev Notes
- Consider `JwtAuthenticationConverter` with a custom `Converter<Jwt, Collection<GrantedAuthority>>` to map roles.
- Audience check can be performed via a `JwtValidator` and `DelegatingOAuth2TokenValidator`.
- Keep `/actuator/**` exposure minimal; avoid leaking build info in prod.
- Avoid adding user PII to logs; log token IDs (jti) only if necessary.
 - Architecture references: see `docs/architecture/security-architecture.md` (security posture, token validation), `docs/architecture/gateway-envoy.md` (edge JWT validation and callback restrictions), and `docs/architecture/api-architecture.md` (API boundaries).

### Risk-Informed Mitigations
- SEC-101 (issuer/audience): enforce strict validators; fail closed; include negative integration tests.
- SEC-102 (role mapping): centralize claim normalization; unit-test Keycloak/Auth0/Okta shapes; deny by default.
- OPS-101 (JWKS outage/rotation): rely on Nimbus JWKS caching; document rotation and add alerts on 401 spikes.
- OPS-102 (clock skew): set skew window (default 60s) and add boundary tests.
- DATA-101 (sensitive logs): redact token, avoid logging claims; prefer request IDs.

### QA References
- Risk profile: `docs/qa/assessments/16.08-risk-20250920.md`
- Test design: `docs/qa/assessments/16.08-test-design-20250920.md`

### Dependencies (Gradle)
```
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
testImplementation("org.springframework.security:spring-security-test")
```

### Example SecurityConfig (outline)
```
@Configuration
@EnableMethodSecurity
class SecurityConfig {
  @Bean
  SecurityFilterChain security(HttpSecurity http, Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth -> oauth
        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
      );
    return http.build();
  }
}
```

### Example Authorities Converter (outline)
```
class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    var roles = new ArrayList<String>();
    var realm = (Map<String, Object>) jwt.getClaim("realm_access");
    if (realm != null) roles.addAll((Collection<String>) realm.getOrDefault("roles", List.of()));
    roles.addAll((Collection<String>) (jwt.getClaim("permissions") != null ? jwt.getClaim("permissions") : List.of()));
    roles.addAll((Collection<String>) (jwt.getClaim("roles") != null ? jwt.getClaim("roles") : List.of()));
    return roles.stream()
      .filter(Objects::nonNull)
      .map(r -> r.replace(':', '_').replace('-', '_').toUpperCase(Locale.ROOT))
      .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
      .collect(Collectors.toSet());
  }
}
```

### Example application.yml (env mapping)
```
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI}
          jwk-set-uri: ${SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI:}

security:
  jwt:
    expected-audience: ${SECURITY_JWT_EXPECTED_AUDIENCE:synergyflow-api}
```

### Audience Validator (outline)
```
@Bean
OAuth2TokenValidator<Jwt> audienceValidator(@Value("${security.jwt.expected-audience}") String aud) {
  return jwt -> jwt.getAudience().contains(aud)
    ? OAuth2TokenValidatorResult.success()
    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience missing", null));
}
```

## Testing
- Unit: authorities converter given example claims produces authorities:
  - Keycloak: `{"realm_access":{"roles":["user","admin"]}}` → `ROLE_USER`, `ROLE_ADMIN`.
  - Auth0: `{"permissions":["read:things","write:things"]}` → `ROLE_READ_THINGS`, `ROLE_WRITE_THINGS` (or `SCOPE_` mapping, document decision).
- Integration (SpringBootTest/WebMvcTest):
  - Inject signed JWT via `Authorization: Bearer <token>` header.
  - `/api/whoami` returns 200 with subject and roles when token valid.
  - `/api/admin/ping` returns 403 if `ROLE_ADMIN` missing.
  - Expired token → 401.
  - Wrong issuer/audience → 401; jwk-set-uri variant accepted when configured.

### Test Design Reference
- Matrix: `docs/qa/assessments/16.08-test-design-20250920.md`
- P0 focus: issuer/audience validation, route protection, admin role, exp/nbf handling.

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-20 | 0.1     | Initial draft — OAuth2 Resource Server (JWT) | PM     |
| 2025-09-20 | 0.2     | PO validation — Approved (Ready for Dev)     | PO     |
| 2025-09-20 | 0.3     | PO course-correction — added deps, config and test specifics | PO     |
| 2025-09-21 | 0.4     | QA remediation — correlation IDs, CORS allowlist, JWT hardening tests | Dev    |

## Dev Agent Record

### Agent Model Used
Claude Opus 4.1 (claude-opus-4-1-20250805)

### Debug Log References
- Fixed compilation errors in integration tests (audience format, authorities method usage)
- Discovered existing implementation already in user module; avoided duplicating SecurityConfig
- `./gradlew test --tests "io.monosense.synergyflow.user.application.WhoAmIControllerIntegrationTest"`

### Completion Notes List
- Added `CorrelationIdFilter` to propagate `X-Correlation-ID` via MDC and response headers per AC4.
- Applied configurable clock skew through `JwtTimestampValidator` ensuring tolerance on exp/nbf (AC6).
- Refined CORS policy to derive allowlists from `NEXTAUTH_URL` and optional `SECURITY_CORS_ALLOWED_ORIGINS` (AC4).
- Expanded `WhoAmIControllerIntegrationTest` to cover invalid audience, expired token, correlation echo, and CORS preflight paths (AC8).
- Introduced actuator stub controller for slice tests and documented new configuration knobs in README and `application.yml`.

### File List
- synergyflow-backend/src/main/java/io/monosense/synergyflow/user/infrastructure/SecurityConfig.java
- synergyflow-backend/src/main/java/io/monosense/synergyflow/user/infrastructure/CorrelationIdFilter.java
- synergyflow-backend/src/main/resources/application.yml
- synergyflow-backend/src/test/java/io/monosense/synergyflow/user/application/ActuatorStubController.java
- synergyflow-backend/src/test/java/io/monosense/synergyflow/user/application/WhoAmIControllerIntegrationTest.java
- synergyflow-backend/README.md

## QA Results
### Review Date: 2025-09-21

### Reviewed By: Quinn (Test Architect)

### Gate Decision
- Status: PASS
- Rationale: Correlation ID propagation, clock-skew tolerance, scoped CORS, and negative JWT coverage are now fully implemented and validated.

### Findings
- None.

### Recommendations
- None.
