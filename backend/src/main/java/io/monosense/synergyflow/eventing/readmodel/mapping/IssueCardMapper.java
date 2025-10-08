package io.monosense.synergyflow.eventing.readmodel.mapping;

import io.monosense.synergyflow.eventing.worker.model.IssueCreatedMessage;
import io.monosense.synergyflow.eventing.worker.model.IssueStateChangedMessage;
import io.monosense.synergyflow.eventing.readmodel.IssueCardEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IssueCardMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "issueId", source = "message.issueId")
    @Mapping(target = "issueKey", source = "message.issueKey")
    @Mapping(target = "title", source = "message.title")
    @Mapping(target = "issueType", source = "message.issueType")
    @Mapping(target = "status", source = "message.status")
    @Mapping(target = "priority", source = "message.priority")
    @Mapping(target = "storyPoints", ignore = true)
    @Mapping(target = "assigneeName", ignore = true)
    @Mapping(target = "assigneeEmail", ignore = true)
    @Mapping(target = "sprintName", ignore = true)
    @Mapping(target = "boardColumn", ignore = true)
    @Mapping(target = "blocked", constant = "false")
    @Mapping(target = "blockerCount", constant = "0")
    @Mapping(target = "parentIssueKey", ignore = true)
    @Mapping(target = "createdAt", source = "message.createdAt")
    @Mapping(target = "updatedAt", source = "message.updatedAt")
    @Mapping(target = "version", source = "version")
    IssueCardEntity toIssueCard(IssueCreatedMessage message, Long version);

    @AfterMapping
    default void ensureDefaults(@MappingTarget IssueCardEntity entity) {
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
        if (entity.getUpdatedAt() == null) {
            entity.setUpdatedAt(entity.getCreatedAt());
        }
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "status", source = "message.toState")
    @Mapping(target = "updatedAt", source = "message.changedAt")
    @Mapping(target = "version", source = "message.version")
    void applyStateChange(@MappingTarget IssueCardEntity entity, IssueStateChangedMessage message);
}
