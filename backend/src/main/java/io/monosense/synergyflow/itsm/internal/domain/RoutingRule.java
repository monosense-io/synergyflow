package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import static org.hibernate.annotations.UuidGenerator.Style;

/**
 * Represents a rule for automatic ticket routing.
 *
 * <p>Routing rules define conditions under which tickets are automatically assigned
 * to specific teams or agents. Rules are evaluated in priority order (lowest number first)
 * and can be enabled or disabled without deletion for operational flexibility.</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>UUIDv7 primary key for time-ordered identifiers</li>
 *   <li>Priority-based ordering for rule evaluation sequence</li>
 *   <li>Support for multiple routing strategies (category, priority, round-robin)</li>
 *   <li>Optimistic locking for concurrent rule updates</li>
 *   <li>Automatic timestamp management for audit trails</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
@Entity
@Table(name = "routing_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutingRule {

    /**
     * UUIDv7 primary key providing time-ordered identifiers.
     *
     * <p>UUIDv7 includes a 48-bit Unix timestamp prefix enabling chronological ordering
     * and improved database index locality. Hibernate 7.0+ generates these automatically.</p>
     */
    @Id
    @UuidGenerator(style = Style.VERSION_7)
    private UUID id;

    /**
     * Human-readable name for this routing rule.
     *
     * <p>Examples: "Critical Incidents to Network Team", "Software Requests to L1 Support"</p>
     */
    @NotBlank(message = "Rule name is required")
    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;

    /**
     * The type of condition to evaluate for routing.
     *
     * <p>Determines how the {@code conditionValue} is interpreted. See {@link ConditionType}
     * for available types.</p>
     */
    @NotNull(message = "Condition type is required")
    @Column(name = "condition_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConditionType conditionType;

    /**
     * The value to match for this condition.
     *
     * <p>Interpretation depends on {@code conditionType}:</p>
     * <ul>
     *   <li>CATEGORY: The category name to match (e.g., "Network")</li>
     *   <li>PRIORITY: The priority level to match (e.g., "CRITICAL")</li>
     *   <li>SUBCATEGORY: The subcategory identifier to match</li>
     *   <li>ROUND_ROBIN: Not used (null)</li>
     * </ul>
     */
    @Column(name = "condition_value", length = 100)
    private String conditionValue;

    /**
     * UUID of the target team to route matching tickets to.
     *
     * <p>Either {@code targetTeamId} or {@code targetAgentId} should be specified, not both.
     * Nullable to support agent-specific routing.</p>
     */
    @Column(name = "target_team_id")
    private UUID targetTeamId;

    /**
     * UUID of the target agent to route matching tickets to.
     *
     * <p>Either {@code targetTeamId} or {@code targetAgentId} should be specified, not both.
     * Nullable to support team-based routing.</p>
     */
    @Column(name = "target_agent_id")
    private UUID targetAgentId;

    /**
     * Priority for rule evaluation order.
     *
     * <p>Lower numbers are evaluated first. Rules with the same priority are evaluated
     * in creation order (UUIDv7 lexicographical ordering).</p>
     */
    @Column(nullable = false)
    private int priority;

    /**
     * Indicates whether this rule is currently active.
     *
     * <p>Disabled rules are retained in the database but not evaluated during routing.
     * This allows temporary rule deactivation without deletion.</p>
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Optimistic locking version field.
     *
     * <p>JPA automatically increments this on each update. Initial value is 1.</p>
     */
    @Version
    private Long version = 1L;

    /**
     * Timestamp when this rule was first persisted.
     *
     * <p>Set automatically by {@link #onCreate()} callback. Immutable after creation.</p>
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the last update to this rule.
     *
     * <p>Set automatically by {@link #onCreate()} and {@link #onUpdate()} callbacks.</p>
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Creates a new routing rule.
     *
     * <p>Initializes a rule with the specified details. The ID is generated automatically
     * by Hibernate using UUIDv7. Timestamps are managed by lifecycle callbacks.</p>
     *
     * @param ruleName human-readable name for this rule (required)
     * @param conditionType the type of condition to evaluate (required)
     * @param conditionValue the value to match (nullable for ROUND_ROBIN)
     * @param targetTeamId the UUID of the target team (nullable if routing to agent)
     * @param targetAgentId the UUID of the target agent (nullable if routing to team)
     * @param priority evaluation order (lower numbers first, required)
     * @param enabled whether this rule is active (required)
     * @since 2.1
     */
    public RoutingRule(String ruleName, ConditionType conditionType, String conditionValue,
                       UUID targetTeamId, UUID targetAgentId, int priority, boolean enabled) {
        this.ruleName = ruleName;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.targetTeamId = targetTeamId;
        this.targetAgentId = targetAgentId;
        this.priority = priority;
        this.enabled = enabled;
    }

    /**
     * JPA lifecycle callback invoked before entity persistence.
     *
     * <p>Sets both {@code createdAt} and {@code updatedAt} to the current instant.</p>
     *
     * @since 2.1
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA lifecycle callback invoked before entity updates.
     *
     * <p>Updates {@code updatedAt} to the current instant. {@code createdAt} remains immutable.</p>
     *
     * @since 2.1
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Enables this routing rule.
     *
     * <p>The {@code updatedAt} timestamp will be updated automatically by the
     * {@link #onUpdate()} callback when the entity is saved.</p>
     *
     * @since 2.1
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disables this routing rule.
     *
     * <p>The {@code updatedAt} timestamp will be updated automatically by the
     * {@link #onUpdate()} callback when the entity is saved.</p>
     *
     * @since 2.1
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Updates the priority of this routing rule.
     *
     * <p>The {@code updatedAt} timestamp will be updated automatically by the
     * {@link #onUpdate()} callback when the entity is saved.</p>
     *
     * @param priority the new priority value (lower numbers evaluated first)
     * @since 2.1
     */
    public void updatePriority(int priority) {
        this.priority = priority;
    }

    /**
     * Optional association to the target team specified by {@code target_team_id}.
     *
     * <p>Mapped as read-only to avoid duplicate source-of-truth with targetTeamId.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_team_id", insertable = false, updatable = false)
    private Team team;
}
