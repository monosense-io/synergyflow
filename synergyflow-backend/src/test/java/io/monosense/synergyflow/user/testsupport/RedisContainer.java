package io.monosense.synergyflow.user.testsupport;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers wrapper for Redis.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class RedisContainer extends GenericContainer<RedisContainer> {
    
    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;
    
    public RedisContainer() {
        super(DockerImageName.parse(REDIS_IMAGE));
        withExposedPorts(REDIS_PORT);
    }
    
    public Integer getRedisPort() {
        return getMappedPort(REDIS_PORT);
    }
    
    public String getRedisHost() {
        return getHost();
    }
}