package io.monosense.synergyflow.user;

import io.monosense.synergyflow.user.testsupport.RedisContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.modulith.test.ApplicationModuleTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Testcontainers setup.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@ApplicationModuleTest
class TestcontainersIntegrationTests {

    @Test
    void shouldStartRedisContainer() {
        try (RedisContainer redis = new RedisContainer()) {
            // Start will throw if Docker is not available
            redis.start();
            
            assertThat(redis.getRedisHost()).isNotNull();
            assertThat(redis.getRedisPort()).isNotNull();
        } catch (IllegalStateException e) {
            // Skip test if Docker is not available
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Docker not available: " + e.getMessage());
        }
    }
}