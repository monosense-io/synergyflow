package io.monosense.synergyflow.testinfra;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Provides a RedissonClient bean for integration tests, pointing to the Redis instance
 * managed outside the JVM (docker-compose).
 */
@Configuration(proxyBeanMethods = false)
class TestRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RedissonClient redissonClient(Environment environment) {
        String host = environment.getProperty("spring.redis.host");
        Integer port = environment.getProperty("spring.redis.port", Integer.class);
        String password = environment.getProperty("spring.redis.password");

        if (!StringUtils.hasText(host)) {
            host = "127.0.0.1";
        }
        int portNumber = (port == null || port <= 0) ? 6379 : port;

        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + portNumber)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2)
                .setSubscriptionConnectionPoolSize(1);

        if (StringUtils.hasText(password)) {
            serverConfig.setPassword(password);
        }

        return Redisson.create(config);
    }

    @Bean
    DisposableBean redissonShutdownHandler(RedissonClient redissonClient) {
        return () -> {
            if (!redissonClient.isShuttingDown() && !redissonClient.isShutdown()) {
                redissonClient.shutdown();
            }
        };
    }
}
