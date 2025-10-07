package io.monosense.synergyflow.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.monosense.synergyflow.security.api.JwtClaims;
import io.monosense.synergyflow.security.api.JwtValidator;
import io.monosense.synergyflow.security.api.RoleMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    private static final RSAKey RSA_JWK = generateRsaKey();
    private static final MockWebServer MOCK_OIDC_SERVER = createMockOidcServer();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtValidator jwtValidator;

    @Autowired
    private RoleMapper roleMapper;

    @AfterAll
    void tearDown() {
        try {
            MOCK_OIDC_SERVER.shutdown();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to shut down mock OIDC server", e);
        }
    }

    @DynamicPropertySource
    static void overrideOauthProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", SecurityIntegrationTest::issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", SecurityIntegrationTest::jwkSetUri);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:security-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Test
    @DisplayName("Authenticated request to /actuator/health returns 200 and maps roles correctly")
    void authenticatedRequestShouldSucceed() {
        String token = createJwtToken(List.of("itsm_agent"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
            "/actuator/health",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");

        JwtClaims claims = jwtValidator.validateAndExtractClaims(token);
        assertThat(claims.subject()).isEqualTo("user-123");
        assertThat(claims.email()).isEqualTo("user@test.com");

        Set<String> mappedRoles = roleMapper.mapKeycloakRolesToAppRoles(claims.roles());
        assertThat(mappedRoles).containsExactly("ITSM_AGENT");
    }

    @Test
    @DisplayName("Request without JWT token is rejected with 401 Unauthorized")
    void unauthenticatedRequestShouldFail() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static String issuerUri() {
        return "http://localhost:" + MOCK_OIDC_SERVER.getPort() + "/realms/synergyflow";
    }

    private static String jwkSetUri() {
        return issuerUri() + "/protocol/openid-connect/certs";
    }

    private static String createJwtToken(List<String> roles) {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuerUri())
            .subject("user-123")
            .claim("email", "user@test.com")
            .claim("preferred_username", "user")
            .claim("realm_access", Map.of("roles", roles))
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(1_800)))
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(RSA_JWK.getKeyID())
            .build();

        try {
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new RSASSASigner(RSA_JWK.toPrivateKey()));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT for integration test", e);
        }
    }

    private static RSAKey generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("integration-test-key")
                .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }

    private static MockWebServer createMockOidcServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start mock OIDC server", e);
        }

        JWKSet jwkSet = new JWKSet(RSA_JWK.toPublicJWK());

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("/realms/synergyflow/protocol/openid-connect/certs".equals(request.getPath())) {
                    return new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setBody(jwkSet.toString());
                }

                return new MockResponse().setResponseCode(404);
            }
        });

        return server;
    }
}
