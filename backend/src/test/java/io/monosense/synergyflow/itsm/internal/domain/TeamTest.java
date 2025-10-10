package io.monosense.synergyflow.itsm.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Team Entity Tests")
class TeamTest {

    @Test
    @DisplayName("Entity creation sets fields via constructor")
    void testConstructor() {
        Team team = new Team("Network Team", "Handles network issues", true);
        assertThat(team.getId()).isNull();
        assertThat(team.getName()).isEqualTo("Network Team");
        assertThat(team.getDescription()).isEqualTo("Handles network issues");
        assertThat(team.isEnabled()).isTrue();
        assertThat(team.getCreatedAt()).isNull();
        assertThat(team.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("@PrePersist sets createdAt and updatedAt")
    void testPrePersist() throws Exception {
        Team team = new Team("Software Team", "App issues", true);
        Method onCreate = Team.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        Instant before = Instant.now();
        onCreate.invoke(team);
        Instant after = Instant.now();

        assertThat(team.getCreatedAt()).isBetween(before, after);
        assertThat(team.getUpdatedAt()).isBetween(before, after);
        assertThat(team.getCreatedAt()).isEqualTo(team.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate updates only updatedAt")
    void testPreUpdate() throws Exception {
        Team team = new Team("Hardware Team", "Devices", true);

        Method onCreate = Team.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(team);

        Instant created = team.getCreatedAt();
        Instant updated1 = team.getUpdatedAt();
        Thread.sleep(5L);

        Method onUpdate = Team.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);
        onUpdate.invoke(team);

        assertThat(team.getCreatedAt()).isEqualTo(created);
        assertThat(team.getUpdatedAt()).isAfter(updated1);
    }
}

