package io.monosense.synergyflow.eventing.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketCardRepository extends JpaRepository<TicketCardEntity, UUID>, TicketCardRepositoryCustom {

    Optional<TicketCardEntity> findByTicketId(UUID ticketId);

    // Custom least-loaded agent selection implemented in TicketCardRepositoryImpl
}
