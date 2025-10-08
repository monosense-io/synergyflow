package io.monosense.synergyflow.eventing.internal.worker;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot configuration class for the worker profile that enables scheduling capabilities.
 *
 * <p>This configuration is automatically activated when the {@code "worker"} Spring profile is active.
 * It enables Spring's {@code @EnableScheduling} annotation to support scheduled task execution
 * for the event processing runtime, particularly for:
 * <ul>
 *   <li>Periodic outbox polling via {@link OutboxPoller}</li>
 *   <li>Event processing and publishing to Redis streams</li>
 *   <li>Read model updates and synchronization</li>
 *   <li>Gap detection and recovery mechanisms</li>
 * </ul>
 *
 * <p>This configuration is essential for the worker microservice that handles asynchronous
 * event processing in the distributed system architecture. It ensures that scheduled jobs
 * can run in the background to process events from the outbox table and update read models.
 *
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * // Activate worker profile
 * @SpringBootApplication
 * public class WorkerApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(WorkerApplication.class, args);
 *     }
 * }
 * }</pre>
 *
 * <p>When running with the worker profile, this configuration automatically enables
 * the scheduling infrastructure required for:
 * <ul>
 *   <li>Polling the outbox table for unprocessed events</li>
 *   <li>Publishing events to Redis streams for downstream consumers</li>
 *   <li>Updating read models based on processed events</li>
 *   <li>Monitoring and detecting gaps in event processing</li>
 * </ul>
 *
 * @author monosense
 * @since 1.6
 * @version 1.0.0
 * @see OutboxPoller
 * @see RedisStreamPublisher
 * @see ReadModelUpdater
 */
@Configuration
@Profile("worker")
@EnableScheduling
class WorkerProfileConfiguration {
}
