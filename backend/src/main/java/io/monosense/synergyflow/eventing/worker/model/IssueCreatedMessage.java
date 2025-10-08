package io.monosense.synergyflow.eventing.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueCreatedMessage(
        UUID issueId,
        String issueKey,
        String title,
        String issueType,
        String status,
        String priority,
        UUID reporterId,
        String reporterName,
        String reporterEmail,
        Instant createdAt,
        Instant updatedAt
) {
}
