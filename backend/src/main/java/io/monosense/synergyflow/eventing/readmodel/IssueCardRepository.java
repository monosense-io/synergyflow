package io.monosense.synergyflow.eventing.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IssueCardRepository extends JpaRepository<IssueCardEntity, UUID>, IssueCardRepositoryCustom {

    Optional<IssueCardEntity> findByIssueId(UUID issueId);
}
