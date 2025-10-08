package io.monosense.synergyflow.eventing.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QueueRowRepository extends JpaRepository<QueueRowEntity, UUID>, QueueRowRepositoryCustom {

    Optional<QueueRowEntity> findByTicketId(UUID ticketId);
}
