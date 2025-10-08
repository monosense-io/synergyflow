package io.monosense.synergyflow.eventing.readmodel.mapping;

import io.monosense.synergyflow.eventing.worker.model.IssueCreatedMessage;
import io.monosense.synergyflow.eventing.readmodel.QueueRowEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface QueueRowMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "ticketId", source = "message.issueId")
    @Mapping(target = "ticketNumber", source = "message.issueKey")
    @Mapping(target = "title", source = "message.title")
    @Mapping(target = "priority", source = "message.priority")
    @Mapping(target = "status", source = "message.status")
    @Mapping(target = "queueName", constant = "PM_DEFAULT")
    @Mapping(target = "queuePosition", constant = "0")
    @Mapping(target = "assignedTeamName", ignore = true)
    @Mapping(target = "waitTimeMinutes", constant = "0")
    @Mapping(target = "slaBreached", constant = "false")
    @Mapping(target = "autoRouteScore", ignore = true)
    @Mapping(target = "createdAt", source = "message.createdAt")
    @Mapping(target = "version", source = "version")
    QueueRowEntity toQueueRow(IssueCreatedMessage message, Long version);

    @AfterMapping
    default void ensureDefaults(@MappingTarget QueueRowEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            entity.setTitle("Untitled");
        }
        if (entity.getPriority() == null || entity.getPriority().isBlank()) {
            entity.setPriority("MEDIUM");
        }
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("TODO");
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        if (entity.getQueueName() == null || entity.getQueueName().isBlank()) {
            entity.setQueueName("PM_DEFAULT");
        }
        if (entity.getQueuePosition() == null) {
            entity.setQueuePosition(0);
        }
        if (entity.getWaitTimeMinutes() == null) {
            entity.setWaitTimeMinutes(0);
        }
    }
}
