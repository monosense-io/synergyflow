package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for RoutingRule entity persistence.
 *
 * <p>Provides database access operations for RoutingRule entities using UUIDv7 as the primary key.
 * This repository extends JpaRepository to inherit standard CRUD operations and includes
 * custom query methods for retrieving active routing rules in priority order.</p>
 *
 * <p>Custom query methods support:
 * <ul>
 *   <li>Retrieval of enabled routing rules sorted by priority (lowest priority value = highest precedence)</li>
 * </ul>
 *
 * <p>Public visibility required for Spring dependency injection across internal packages.</p>
 *
 * @author monosense
 * @since 2.1
 */
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    /**
     * Finds all enabled routing rules, ordered by priority (lowest priority value first).
     *
     * <p>This method returns only rules where enabled=true, sorted by the priority field in
     * ascending order. Lower priority values indicate higher precedence in rule evaluation.
     * For example, a rule with priority=1 is evaluated before a rule with priority=10.</p>
     *
     * <p>This query is used by the routing engine to determine ticket assignment based on
     * category, priority, or other conditions defined in the rules.</p>
     *
     * @return list of enabled routing rules in priority order, or empty list if no enabled rules exist
     * @since 2.1
     */
    List<RoutingRule> findByEnabledTrueOrderByPriorityAsc();
}
