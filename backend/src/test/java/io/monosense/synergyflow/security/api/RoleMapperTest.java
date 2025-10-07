package io.monosense.synergyflow.security.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleMapper Unit Tests")
class RoleMapperTest {

    private RoleMapper roleMapper;

    @BeforeEach
    void setUp() {
        roleMapper = new RoleMapper();
    }

    @Test
    @DisplayName("Should map admin role correctly")
    void shouldMapAdminRole() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of("admin"));

        // Then
        assertThat(appRoles).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Should map itsm_agent role correctly")
    void shouldMapItsmAgentRole() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of("itsm_agent"));

        // Then
        assertThat(appRoles).containsExactly("ITSM_AGENT");
    }

    @Test
    @DisplayName("Should map developer role correctly")
    void shouldMapDeveloperRole() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of("developer"));

        // Then
        assertThat(appRoles).containsExactly("DEVELOPER");
    }

    @Test
    @DisplayName("Should map manager role correctly")
    void shouldMapManagerRole() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of("manager"));

        // Then
        assertThat(appRoles).containsExactly("MANAGER");
    }

    @Test
    @DisplayName("Should map employee role correctly")
    void shouldMapEmployeeRole() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of("employee"));

        // Then
        assertThat(appRoles).containsExactly("EMPLOYEE");
    }

    @Test
    @DisplayName("Should map multiple roles correctly")
    void shouldMapMultipleRoles() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(
            List.of("admin", "itsm_agent", "developer")
        );

        // Then
        assertThat(appRoles).containsExactlyInAnyOrder("ADMIN", "ITSM_AGENT", "DEVELOPER");
    }

    @ParameterizedTest
    @MethodSource("caseInsensitiveRoleProvider")
    @DisplayName("Should handle case-insensitive role matching")
    void shouldHandleCaseInsensitiveMatching(String keycloakRole, String expectedAppRole) {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of(keycloakRole));

        // Then
        assertThat(appRoles).containsExactly(expectedAppRole);
    }

    static Stream<Arguments> caseInsensitiveRoleProvider() {
        return Stream.of(
            Arguments.of("ADMIN", "ADMIN"),
            Arguments.of("Admin", "ADMIN"),
            Arguments.of("aDmIn", "ADMIN"),
            Arguments.of("ITSM_AGENT", "ITSM_AGENT"),
            Arguments.of("Itsm_Agent", "ITSM_AGENT"),
            Arguments.of("DEVELOPER", "DEVELOPER"),
            Arguments.of("Developer", "DEVELOPER"),
            Arguments.of("MANAGER", "MANAGER"),
            Arguments.of("Manager", "MANAGER"),
            Arguments.of("EMPLOYEE", "EMPLOYEE"),
            Arguments.of("Employee", "EMPLOYEE")
        );
    }

    @Test
    @DisplayName("Should skip unknown roles and log warning")
    void shouldSkipUnknownRoles() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(
            List.of("admin", "unknown_role", "itsm_agent", "invalid_role")
        );

        // Then
        assertThat(appRoles).containsExactlyInAnyOrder("ADMIN", "ITSM_AGENT");
    }

    @Test
    @DisplayName("Should return empty set when input is null")
    void shouldReturnEmptySetWhenInputIsNull() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(null);

        // Then
        assertThat(appRoles).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when input is empty list")
    void shouldReturnEmptySetWhenInputIsEmpty() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(List.of());

        // Then
        assertThat(appRoles).isEmpty();
    }

    @Test
    @DisplayName("Should skip null and blank roles")
    void shouldSkipNullAndBlankRoles() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(
            Arrays.asList("admin", null, "itsm_agent", "", "  ", "developer")
        );

        // Then
        assertThat(appRoles).containsExactlyInAnyOrder("ADMIN", "ITSM_AGENT", "DEVELOPER");
    }

    @Test
    @DisplayName("Should not contain duplicate roles")
    void shouldNotContainDuplicateRoles() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(
            List.of("admin", "ADMIN", "Admin", "itsm_agent", "ITSM_AGENT")
        );

        // Then
        assertThat(appRoles).containsExactlyInAnyOrder("ADMIN", "ITSM_AGENT");
    }

    @Test
    @DisplayName("Should map all application roles")
    void shouldMapAllApplicationRoles() {
        // When
        Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(
            List.of("admin", "itsm_agent", "developer", "manager", "employee")
        );

        // Then
        assertThat(appRoles).containsExactlyInAnyOrder(
            "ADMIN", "ITSM_AGENT", "DEVELOPER", "MANAGER", "EMPLOYEE"
        );
    }
}
