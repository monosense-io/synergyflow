package io.monosense.synergyflow.testsupport.containers;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;

/**
 * ApplicationContextInitializer that starts reusable Testcontainers before Spring context initialization.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        // Start all containers
        PostgreSQLReusableContainer.getInstance().start();
        RedisReusableContainer.getInstance().start();
        KafkaReusableContainer.getInstance().start();
        
        // Set properties for Spring to use
        TestPropertyValues.of(
            "spring.datasource.url=" + System.getProperty("TEST_DB_URL"),
            "spring.datasource.username=" + System.getProperty("TEST_DB_USERNAME"),
            "spring.datasource.password=" + System.getProperty("TEST_DB_PASSWORD"),
            "spring.redis.host=" + System.getProperty("TEST_REDIS_HOST"),
            "spring.redis.port=" + System.getProperty("TEST_REDIS_PORT"),
            "spring.kafka.bootstrap-servers=" + System.getProperty("TEST_KAFKA_BOOTSTRAP_SERVERS")
        ).applyTo(applicationContext.getEnvironment());
    }
}