package io.monosense.synergyflow.eventing.readmodel;

import java.time.Instant;
import java.util.UUID;

/**
 * Custom repository extension exposing version-guarded operations for issue cards.
 */
public interface IssueCardRepositoryCustom {

    boolean upsertWithVersionGuard(IssueCardEntity entity);

    boolean updateStateWithVersionGuard(UUID issueId, String status, Instant updatedAt, long version);
}

