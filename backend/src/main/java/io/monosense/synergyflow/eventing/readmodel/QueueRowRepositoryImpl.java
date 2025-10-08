package io.monosense.synergyflow.eventing.readmodel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class QueueRowRepositoryImpl implements QueueRowRepositoryCustom {

    private static final String UPSERT_SQL = """
        INSERT INTO queue_row (
            id,
            ticket_id,
            ticket_number,
            title,
            priority,
            status,
            queue_name,
            queue_position,
            assigned_team_name,
            wait_time_minutes,
            sla_breached,
            auto_route_score,
            created_at,
            version
        ) VALUES (
            :id,
            :ticketId,
            :ticketNumber,
            :title,
            :priority,
            :status,
            :queueName,
            :queuePosition,
            :assignedTeamName,
            :waitTimeMinutes,
            :slaBreached,
            :autoRouteScore,
            :createdAt,
            :version
        )
        ON CONFLICT (ticket_id)
        DO UPDATE SET
            ticket_number = EXCLUDED.ticket_number,
            title = EXCLUDED.title,
            priority = EXCLUDED.priority,
            status = EXCLUDED.status,
            queue_name = EXCLUDED.queue_name,
            queue_position = EXCLUDED.queue_position,
            assigned_team_name = EXCLUDED.assigned_team_name,
            wait_time_minutes = EXCLUDED.wait_time_minutes,
            sla_breached = EXCLUDED.sla_breached,
            auto_route_score = EXCLUDED.auto_route_score,
            created_at = queue_row.created_at,
            version = EXCLUDED.version
        WHERE queue_row.version < EXCLUDED.version
        """;

    private static final String UPDATE_STATE_SQL = """
        UPDATE queue_row
        SET status = :status,
            version = :version
        WHERE ticket_id = :ticketId AND version < :version
        """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean upsertWithVersionGuard(QueueRowEntity entity) {
        int affected = entityManager.createNativeQuery(UPSERT_SQL)
                .setParameter("id", entity.getId())
                .setParameter("ticketId", entity.getTicketId())
                .setParameter("ticketNumber", entity.getTicketNumber())
                .setParameter("title", entity.getTitle())
                .setParameter("priority", entity.getPriority())
                .setParameter("status", entity.getStatus())
                .setParameter("queueName", entity.getQueueName())
                .setParameter("queuePosition", entity.getQueuePosition())
                .setParameter("assignedTeamName", entity.getAssignedTeamName())
                .setParameter("waitTimeMinutes", entity.getWaitTimeMinutes())
                .setParameter("slaBreached", entity.isSlaBreached())
                .setParameter("autoRouteScore", entity.getAutoRouteScore())
                .setParameter("createdAt", entity.getCreatedAt())
                .setParameter("version", entity.getVersion())
                .executeUpdate();
        return affected > 0;
    }

    @Override
    public boolean updateStateWithVersionGuard(UUID ticketId, String status, long version) {
        int affected = entityManager.createNativeQuery(UPDATE_STATE_SQL)
                .setParameter("ticketId", ticketId)
                .setParameter("status", status)
                .setParameter("version", version)
                .executeUpdate();
        return affected > 0;
    }
}

