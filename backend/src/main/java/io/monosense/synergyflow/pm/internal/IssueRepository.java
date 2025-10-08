package io.monosense.synergyflow.pm.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Spring Data JPA repository for Issue aggregate persistence.
 *
 * <p>Provides database access operations for Issue entities using UUID as the primary key.
 * This repository extends JpaRepository to inherit standard CRUD operations and can be
 * extended with custom query methods as needed. The Issue repository manages the lifecycle
 * of Project Management domain issues, supporting the core functionality of issue tracking
 * and state management.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class IssueService {
 *     private final IssueRepository issueRepository;
 *
 *     public Optional<Issue> findIssue(UUID id) {
 *         return issueRepository.findById(id);
 *     }
 *
 *     public List<Issue> findIssuesByStatus(String status) {
 *         return issueRepository.findByStatus(status);
 *     }
 *
 *     public List<Issue> findIssuesByReporter(UUID reporterId) {
 *         return issueRepository.findByReporterId(reporterId);
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
interface IssueRepository extends JpaRepository<Issue, UUID> {
}
