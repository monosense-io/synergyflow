package io.monosense.synergyflow.itsm.internal.domain;

/**
 * Enumeration of routing rule condition types.
 *
 * <p>Defines the types of conditions that can be used to route tickets to teams or agents.
 * Each type determines how the {@code conditionValue} field is interpreted in a
 * {@link RoutingRule}.</p>
 *
 * <ul>
 *   <li>{@link #CATEGORY} - Routes based on ticket category (e.g., "Network", "Hardware")</li>
 *   <li>{@link #PRIORITY} - Routes based on ticket priority (e.g., "CRITICAL", "HIGH")</li>
 *   <li>{@link #SUBCATEGORY} - Routes based on subcategory classification</li>
 *   <li>{@link #ROUND_ROBIN} - Distributes tickets evenly across available agents</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
public enum ConditionType {
    /**
     * Route based on ticket category field.
     *
     * <p>The {@code conditionValue} should match the ticket's category.</p>
     */
    CATEGORY,

    /**
     * Route based on ticket priority level.
     *
     * <p>The {@code conditionValue} should match the ticket's priority enum value
     * (e.g., "CRITICAL", "HIGH", "MEDIUM", "LOW").</p>
     */
    PRIORITY,

    /**
     * Route based on subcategory classification.
     *
     * <p>The {@code conditionValue} should match a specific subcategory identifier.</p>
     */
    SUBCATEGORY,

    /**
     * Distribute tickets using round-robin algorithm.
     *
     * <p>The {@code conditionValue} is not used for this type; tickets are distributed
     * evenly across all available agents in the target team.</p>
     */
    ROUND_ROBIN
}
