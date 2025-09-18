package io.monosense.synergyflow.testsupport.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Redis Testcontainer with pre-configured settings.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class RedisReusableContainer extends GenericContainer<RedisReusableContainer> {
    
    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;
    
    private static RedisReusableContainer container;
    
    private RedisReusableContainer() {
        super(DockerImageName.parse(REDIS_IMAGE));
        withExposedPorts(REDIS_PORT);
        withReuse(true);
    }
    
    public static RedisReusableContainer getInstance() {
        if (container == null) {
            container = new RedisReusableContainer();
        }
        return container;
    }
    
    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_REDIS_HOST", container.getHost());
        System.setProperty("TEST_REDIS_PORT", String.valueOf(container.getMappedPort(REDIS_PORT)));
    }
    
    @Override
    public void stop() {
        // Do nothing, reuse container
    }
    
    public String getRedisHost() {
        return getHost();
    }
    
    public Integer getRedisPort() {
        return getMappedPort(REDIS_PORT);
    }
}