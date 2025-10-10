package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("TeamRepository Integration Test")
class TeamRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TeamRepository teamRepository;

    @BeforeEach
    void setup() {
        // Align session with schema used in other ITs
        jdbc.execute("SET search_path TO it_test");

        // Clean and seed teams
        jdbc.update("TRUNCATE TABLE it_test.teams RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO it_test.teams (id, name, description, enabled, created_at, updated_at) VALUES (?, 'Enabled Team', 'E team', true, now(), now())",
                UUID.randomUUID());
        jdbc.update("INSERT INTO it_test.teams (id, name, description, enabled, created_at, updated_at) VALUES (?, 'Disabled Team', 'D team', false, now(), now())",
                UUID.randomUUID());
    }

    @Test
    @DisplayName("findByEnabledTrue returns only enabled teams")
    void findByEnabledTrue_returnsOnlyEnabled() {
        List<Team> result = teamRepository.findByEnabledTrue();
        assertThat(result)
                .extracting(Team::isEnabled)
                .containsOnly(true);
        assertThat(result)
                .extracting(Team::getName)
                .contains("Enabled Team")
                .doesNotContain("Disabled Team");
    }
}

