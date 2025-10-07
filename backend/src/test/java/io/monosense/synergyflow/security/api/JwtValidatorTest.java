package io.monosense.synergyflow.security.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtValidator Unit Tests")
class JwtValidatorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    private JwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator(jwtDecoder);
    }

    @Test
    @DisplayName("Should extract claims from valid JWT token")
    void shouldExtractClaimsFromValidToken() {
        // Given
        String token = "valid.jwt.token";
        Jwt jwt = createMockJwt(
            "user123",
            "user@test.com",
            "user",
            List.of("admin", "itsm_agent")
        );
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When
        JwtClaims claims = jwtValidator.validateAndExtractClaims(token);

        // Then
        assertThat(claims.subject()).isEqualTo("user123");
        assertThat(claims.email()).isEqualTo("user@test.com");
        assertThat(claims.preferredUsername()).isEqualTo("user");
        assertThat(claims.roles()).containsExactlyInAnyOrder("admin", "itsm_agent");
    }

    @Test
    @DisplayName("Should return empty roles when realm_access claim is missing")
    void shouldReturnEmptyRolesWhenRealmAccessMissing() {
        // Given
        String token = "valid.jwt.token";
        Jwt jwt = createMockJwtWithoutRoles("user123", "user@test.com", "user");
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When
        JwtClaims claims = jwtValidator.validateAndExtractClaims(token);

        // Then
        assertThat(claims.subject()).isEqualTo("user123");
        assertThat(claims.roles()).isEmpty();
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when JWT decoding fails")
    void shouldThrowExceptionWhenDecodingFails() {
        // Given
        String token = "invalid.jwt.token";
        when(jwtDecoder.decode(token)).thenThrow(new JwtException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> jwtValidator.validateAndExtractClaims(token))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("Failed to validate JWT token")
            .hasCauseInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when unexpected error occurs")
    void shouldThrowExceptionWhenUnexpectedErrorOccurs() {
        // Given
        String token = "problematic.jwt.token";
        when(jwtDecoder.decode(token)).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        assertThatThrownBy(() -> jwtValidator.validateAndExtractClaims(token))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessageContaining("Unexpected error during token validation");
    }

    @Test
    @DisplayName("Should handle null values in JWT claims gracefully")
    void shouldHandleNullClaimsGracefully() {
        // Given
        String token = "valid.jwt.token";
        Jwt jwt = Jwt.withTokenValue(token)
            .header("alg", "RS256")
            .claim("sub", "user123")
            .claim("email", null)
            .claim("preferred_username", null)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        // When
        JwtClaims claims = jwtValidator.validateAndExtractClaims(token);

        // Then
        assertThat(claims.subject()).isEqualTo("user123");
        assertThat(claims.email()).isNull();
        assertThat(claims.preferredUsername()).isNull();
        assertThat(claims.roles()).isEmpty();
    }

    // Helper methods

    private Jwt createMockJwt(String subject, String email, String preferredUsername, List<String> roles) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", subject)
            .claim("email", email)
            .claim("preferred_username", preferredUsername)
            .claim("realm_access", Map.of("roles", roles))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    private Jwt createMockJwtWithoutRoles(String subject, String email, String preferredUsername) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", subject)
            .claim("email", email)
            .claim("preferred_username", preferredUsername)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}
