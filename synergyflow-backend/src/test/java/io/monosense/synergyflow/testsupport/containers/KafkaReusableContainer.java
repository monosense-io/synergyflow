package io.monosense.synergyflow.testsupport.containers;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Kafka Testcontainer with pre-configured settings.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class KafkaReusableContainer extends KafkaContainer {
    
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.5.0";
    
    private static KafkaReusableContainer container;
    
    private KafkaReusableContainer() {
        super(DockerImageName.parse(KAFKA_IMAGE));
        withEmbeddedZookeeper();
        withReuse(true);
    }
    
    public static KafkaReusableContainer getInstance() {
        if (container == null) {
            container = new KafkaReusableContainer();
        }
        return container;
    }
    
    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_KAFKA_BOOTSTRAP_SERVERS", container.getBootstrapServers());
    }
    
    @Override
    public void stop() {
        // Do nothing, reuse container
    }
}