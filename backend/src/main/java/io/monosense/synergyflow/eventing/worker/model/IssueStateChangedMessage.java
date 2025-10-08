package io.monosense.synergyflow.eventing.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueStateChangedMessage(
        UUID issueId,
        String fromState,
        String toState,
        UUID changedBy,
        String changedByName,
        String changedByEmail,
        Long version,
        Instant changedAt
) {
}
