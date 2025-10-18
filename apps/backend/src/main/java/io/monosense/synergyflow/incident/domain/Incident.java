package io.monosense.synergyflow.incident.domain;

import com.baomidou.mybatisplus.annotation.*;
import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;

import java.time.Instant;

/**
 * Incident entity (aggregate root).
 *
 * <p>MyBatis-Plus entity with table mapping.
 * Uses PostgreSQL IDENTITY for auto-incrementing primary key.
 */
@TableName("incidents")
public class Incident {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("incident_id")
    private String incidentId; // UUID

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("priority")
    private IncidentCreatedEvent.Priority priority;

    @TableField("status")
    private IncidentStatus status;

    @TableField("reported_by")
    private String reportedBy; // User ID

    @TableField("assigned_to")
    private String assignedTo; // User ID (nullable)

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted; // 0 = active, 1 = deleted

    public enum IncidentStatus {
        NEW,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentCreatedEvent.Priority getPriority() {
        return priority;
    }

    public void setPriority(IncidentCreatedEvent.Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
