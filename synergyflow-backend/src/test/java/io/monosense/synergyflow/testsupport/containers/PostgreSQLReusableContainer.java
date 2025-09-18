package io.monosense.synergyflow.testsupport.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable PostgreSQL Testcontainer with pre-configured settings.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class PostgreSQLReusableContainer extends PostgreSQLContainer<PostgreSQLReusableContainer> {
    
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String DATABASE_NAME = "synergyflow_test";
    private static final String USERNAME = "test_user";
    private static final String PASSWORD = "test_password";
    
    private static PostgreSQLReusableContainer container;
    
    private PostgreSQLReusableContainer() {
        super(DockerImageName.parse(POSTGRES_IMAGE));
        withDatabaseName(DATABASE_NAME);
        withUsername(USERNAME);
        withPassword(PASSWORD);
        withReuse(true);
    }
    
    public static PostgreSQLReusableContainer getInstance() {
        if (container == null) {
            container = new PostgreSQLReusableContainer();
        }
        return container;
    }
    
    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_DB_URL", container.getJdbcUrl());
        System.setProperty("TEST_DB_USERNAME", container.getUsername());
        System.setProperty("TEST_DB_PASSWORD", container.getPassword());
    }
    
    @Override
    public void stop() {
        // Do nothing, reuse container
    }
}