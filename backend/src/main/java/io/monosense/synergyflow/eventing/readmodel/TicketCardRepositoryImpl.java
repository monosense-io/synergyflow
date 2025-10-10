package io.monosense.synergyflow.eventing.readmodel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
class TicketCardRepositoryImpl implements TicketCardRepositoryCustom {

    private static final String UPSERT_SQL = """
        INSERT INTO ticket_card (
            id,
            ticket_id,
            ticket_number,
            title,
            status,
            priority,
            category,
            requester_name,
            requester_email,
            assignee_name,
            assignee_email,
            sla_due_at,
            sla_status,
            is_overdue,
            created_at,
            updated_at,
            version
        ) VALUES (
            :id,
            :ticketId,
            :ticketNumber,
            :title,
            :status,
            :priority,
            :category,
            :requesterName,
            :requesterEmail,
            :assigneeName,
            :assigneeEmail,
            :slaDueAt,
            :slaStatus,
            :isOverdue,
            :createdAt,
            :updatedAt,
            :version
        )
        ON CONFLICT (ticket_id)
        DO UPDATE SET
            ticket_number = EXCLUDED.ticket_number,
            title = EXCLUDED.title,
            status = EXCLUDED.status,
            priority = EXCLUDED.priority,
            category = EXCLUDED.category,
            requester_name = EXCLUDED.requester_name,
            requester_email = EXCLUDED.requester_email,
            assignee_name = EXCLUDED.assignee_name,
            assignee_email = EXCLUDED.assignee_email,
            sla_due_at = EXCLUDED.sla_due_at,
            sla_status = EXCLUDED.sla_status,
            is_overdue = EXCLUDED.is_overdue,
            created_at = ticket_card.created_at,
            updated_at = EXCLUDED.updated_at,
            version = EXCLUDED.version
        WHERE ticket_card.version < EXCLUDED.version
        """;

    private static final String UPDATE_ASSIGNMENT_SQL = """
        UPDATE ticket_card
        SET assignee_name = :assigneeName,
            assignee_email = :assigneeEmail,
            updated_at = :updatedAt,
            version = :version
        WHERE ticket_id = :ticketId AND version < :version
        """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean upsertWithVersionGuard(TicketCardEntity entity) {
        int affected = entityManager.createNativeQuery(UPSERT_SQL)
                .setParameter("id", entity.getId())
                .setParameter("ticketId", entity.getTicketId())
                .setParameter("ticketNumber", entity.getTicketNumber())
                .setParameter("title", entity.getTitle())
                .setParameter("status", entity.getStatus())
                .setParameter("priority", entity.getPriority())
                .setParameter("category", entity.getCategory())
                .setParameter("requesterName", entity.getRequesterName())
                .setParameter("requesterEmail", entity.getRequesterEmail())
                .setParameter("assigneeName", entity.getAssigneeName())
                .setParameter("assigneeEmail", entity.getAssigneeEmail())
                .setParameter("slaDueAt", entity.getSlaDueAt())
                .setParameter("slaStatus", entity.getSlaStatus())
                .setParameter("isOverdue", entity.isOverdue())
                .setParameter("createdAt", entity.getCreatedAt())
                .setParameter("updatedAt", entity.getUpdatedAt())
                .setParameter("version", entity.getVersion())
                .executeUpdate();
        return affected > 0;
    }

    @Override
    public boolean updateAssignmentWithVersionGuard(UUID ticketId,
                                                    String assigneeName,
                                                    String assigneeEmail,
                                                    Instant updatedAt,
                                                    long version) {
        int affected = entityManager.createNativeQuery(UPDATE_ASSIGNMENT_SQL)
                .setParameter("ticketId", ticketId)
                .setParameter("assigneeName", assigneeName)
                .setParameter("assigneeEmail", assigneeEmail)
                .setParameter("updatedAt", updatedAt)
                .setParameter("version", version)
                .executeUpdate();
        return affected > 0;
    }

    @Override
    public java.util.Optional<UUID> findAgentWithFewestTickets(UUID teamId) {
        Object result = entityManager.createNativeQuery("""
                SELECT m.user_id AS agent_id
                FROM team_members m
                LEFT JOIN tickets t
                  ON t.assignee_id = m.user_id
                 AND t.status NOT IN ('RESOLVED','CLOSED')
                WHERE m.team_id = :teamId
                GROUP BY m.user_id
                ORDER BY COUNT(t.id) ASC
                LIMIT 1
                """)
            .setParameter("teamId", teamId)
            .getSingleResult();

        if (result == null) {
            return java.util.Optional.empty();
        }
        if (result instanceof UUID uuid) {
            return java.util.Optional.of(uuid);
        }
        return java.util.Optional.of(java.util.UUID.fromString(result.toString()));
    }
}
