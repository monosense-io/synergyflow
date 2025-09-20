package io.monosense.synergyflow.user.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Spring Security configuration for OAuth2 Resource Server (JWT) within the user module.
 *
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${security.jwt.expected-audience:synergyflow-api}")
    private String expectedAudience;

    @Value("${security.jwt.clock-skew:60}")
    private long clockSkewSeconds;

    @Value("${NEXTAUTH_URL:}")
    private String nextAuthUrl;

    @Value("${security.cors.allowed-origins:}")
    private String additionalAllowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorrelationIdFilter correlationIdFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = audienceValidator();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(clockSkewSeconds));
        OAuth2TokenValidator<Jwt> validatorChain = new DelegatingOAuth2TokenValidator<>(withIssuer, timestampValidator, audienceValidator);

        jwtDecoder.setJwtValidator(validatorChain);

        return jwtDecoder;
    }

    @Bean
    public OAuth2TokenValidator<Jwt> audienceValidator() {
        return new OAuth2TokenValidator<Jwt>() {
            @Override
            public OAuth2TokenValidatorResult validate(Jwt jwt) {
                if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
                    return OAuth2TokenValidatorResult.success();
                }
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience missing", null));
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());
        return jwtConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(resolveAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    private List<String> resolveAllowedOrigins() {
        Set<String> origins = new LinkedHashSet<>();

        if (StringUtils.hasText(nextAuthUrl)) {
            addOriginVariants(origins, normalizeOrigin(nextAuthUrl));
        }

        if (StringUtils.hasText(additionalAllowedOrigins)) {
            Arrays.stream(additionalAllowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizeOrigin)
                .forEach(origin -> addOriginVariants(origins, origin));
        }

        if (origins.isEmpty()) {
            addOriginVariants(origins, "http://localhost:3000");
        }

        return new ArrayList<>(origins);
    }

    private void addOriginVariants(Set<String> origins, String origin) {
        origins.add(origin);
        if (origin.contains("localhost")) {
            origins.add(origin.replace("localhost", "127.0.0.1"));
        }
    }

    private String normalizeOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return origin;
        }
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
