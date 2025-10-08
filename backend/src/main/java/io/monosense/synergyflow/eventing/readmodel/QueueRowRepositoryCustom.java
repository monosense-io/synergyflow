package io.monosense.synergyflow.eventing.readmodel;

import java.util.UUID;

/**
 * Custom repository extension for queue rows with version guarded writes.
 */
public interface QueueRowRepositoryCustom {

    boolean upsertWithVersionGuard(QueueRowEntity entity);

    boolean updateStateWithVersionGuard(UUID ticketId, String status, long version);
}

