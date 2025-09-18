package io.monosense.synergyflow.testsupport.containers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for starting reusable Testcontainers before test execution.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class TestcontainersExtension implements BeforeAllCallback {
    
    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        // Start all containers
        PostgreSQLReusableContainer.getInstance().start();
        RedisReusableContainer.getInstance().start();
        KafkaReusableContainer.getInstance().start();
    }
}