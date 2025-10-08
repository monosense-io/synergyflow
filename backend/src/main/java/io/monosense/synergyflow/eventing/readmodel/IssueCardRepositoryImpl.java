package io.monosense.synergyflow.eventing.readmodel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
class IssueCardRepositoryImpl implements IssueCardRepositoryCustom {

    private static final String UPSERT_SQL = """
        INSERT INTO issue_card (
            id,
            issue_id,
            issue_key,
            title,
            issue_type,
            status,
            priority,
            story_points,
            assignee_name,
            assignee_email,
            sprint_name,
            board_column,
            is_blocked,
            blocker_count,
            parent_issue_key,
            created_at,
            updated_at,
            version
        ) VALUES (
            :id,
            :issueId,
            :issueKey,
            :title,
            :issueType,
            :status,
            :priority,
            :storyPoints,
            :assigneeName,
            :assigneeEmail,
            :sprintName,
            :boardColumn,
            :isBlocked,
            :blockerCount,
            :parentIssueKey,
            :createdAt,
            :updatedAt,
            :version
        )
        ON CONFLICT (issue_id)
        DO UPDATE SET
            issue_key = EXCLUDED.issue_key,
            title = EXCLUDED.title,
            issue_type = EXCLUDED.issue_type,
            status = EXCLUDED.status,
            priority = EXCLUDED.priority,
            story_points = EXCLUDED.story_points,
            assignee_name = EXCLUDED.assignee_name,
            assignee_email = EXCLUDED.assignee_email,
            sprint_name = EXCLUDED.sprint_name,
            board_column = EXCLUDED.board_column,
            is_blocked = EXCLUDED.is_blocked,
            blocker_count = EXCLUDED.blocker_count,
            parent_issue_key = EXCLUDED.parent_issue_key,
            created_at = issue_card.created_at,
            updated_at = EXCLUDED.updated_at,
            version = EXCLUDED.version
        WHERE issue_card.version < EXCLUDED.version
        """;

    private static final String UPDATE_STATE_SQL = """
        UPDATE issue_card
        SET status = :status,
            updated_at = :updatedAt,
            version = :version
        WHERE issue_id = :issueId AND version < :version
        """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean upsertWithVersionGuard(IssueCardEntity entity) {
        int affected = entityManager.createNativeQuery(UPSERT_SQL)
                .setParameter("id", entity.getId())
                .setParameter("issueId", entity.getIssueId())
                .setParameter("issueKey", entity.getIssueKey())
                .setParameter("title", entity.getTitle())
                .setParameter("issueType", entity.getIssueType())
                .setParameter("status", entity.getStatus())
                .setParameter("priority", entity.getPriority())
                .setParameter("storyPoints", entity.getStoryPoints())
                .setParameter("assigneeName", entity.getAssigneeName())
                .setParameter("assigneeEmail", entity.getAssigneeEmail())
                .setParameter("sprintName", entity.getSprintName())
                .setParameter("boardColumn", entity.getBoardColumn())
                .setParameter("isBlocked", entity.isBlocked())
                .setParameter("blockerCount", entity.getBlockerCount())
                .setParameter("parentIssueKey", entity.getParentIssueKey())
                .setParameter("createdAt", entity.getCreatedAt())
                .setParameter("updatedAt", entity.getUpdatedAt())
                .setParameter("version", entity.getVersion())
                .executeUpdate();
        return affected > 0;
    }

    @Override
    public boolean updateStateWithVersionGuard(UUID issueId, String status, Instant updatedAt, long version) {
        int affected = entityManager.createNativeQuery(UPDATE_STATE_SQL)
                .setParameter("issueId", issueId)
                .setParameter("status", status)
                .setParameter("updatedAt", updatedAt)
                .setParameter("version", version)
                .executeUpdate();
        return affected > 0;
    }
}

