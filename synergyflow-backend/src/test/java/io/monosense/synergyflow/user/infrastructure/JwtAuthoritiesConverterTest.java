package io.monosense.synergyflow.user.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtAuthoritiesConverter.
 * Tests role extraction and normalization from various IdP JWT formats.
 *
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@DisplayName("JwtAuthoritiesConverter")
class JwtAuthoritiesConverterTest {

    private JwtAuthoritiesConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthoritiesConverter();
    }

    @Test
    @DisplayName("should extract Keycloak realm roles correctly")
    void shouldExtractKeycloakRealmRoles() {
        // Given
        Map<String, Object> realmAccess = Map.of("roles", List.of("user", "admin"));
        Jwt jwt = createJwt(Map.of("realm_access", realmAccess));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("should extract Auth0/Okta permissions correctly")
    void shouldExtractAuth0OktaPermissions() {
        // Given
        Jwt jwt = createJwt(Map.of(
                "permissions", List.of("read:things", "write:things")
        ));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_READ_THINGS", "ROLE_WRITE_THINGS");
    }

    @Test
    @DisplayName("should extract Auth0/Okta roles correctly")
    void shouldExtractAuth0OktaRoles() {
        // Given
        Jwt jwt = createJwt(Map.of(
                "roles", List.of("user", "moderator")
        ));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_MODERATOR");
    }

    @Test
    @DisplayName("should combine roles from multiple sources")
    void shouldCombineRolesFromMultipleSources() {
        // Given
        Map<String, Object> realmAccess = Map.of("roles", List.of("user"));
        Jwt jwt = createJwt(Map.of(
                "realm_access", realmAccess,
                "permissions", List.of("read:data"),
                "roles", List.of("admin")
        ));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_READ_DATA", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("should normalize role names correctly")
    void shouldNormalizeRoleNames() {
        // Given
        Jwt jwt = createJwt(Map.of(
                "permissions", List.of("read:user-data", "write.config", "admin-role")
        ));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_READ_USER_DATA", "ROLE_WRITE_CONFIG", "ROLE_ADMIN_ROLE");
    }

    @Test
    @DisplayName("should handle empty roles gracefully")
    void shouldHandleEmptyRoles() {
        // Given
        Jwt jwt = createJwt(Map.of());

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("should filter out null and empty roles")
    void shouldFilterOutNullAndEmptyRoles() {
        // Given
        Map<String, Object> realmAccess = Map.of("roles", List.of("user", "", "admin"));
        Jwt jwt = createJwt(Map.of(
                "realm_access", realmAccess,
                "permissions", List.of("read:data", "  ", "write:data")
        ));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_READ_DATA", "ROLE_WRITE_DATA");
    }

    @Test
    @DisplayName("should handle malformed realm_access gracefully")
    void shouldHandleMalformedRealmAccess() {
        // Given
        Jwt jwt = createJwt(Map.of("realm_access", "invalid"));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("should deduplicate roles")
    void shouldDeduplicateRoles() {
        // Given
        Map<String, Object> realmAccess = Map.of("roles", List.of("user", "admin"));
        Jwt jwt = createJwt(Map.of(
                "realm_access", realmAccess,
                "roles", List.of("user", "guest")
        ));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_GUEST");
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-user")
                .issuer("test-issuer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }
}