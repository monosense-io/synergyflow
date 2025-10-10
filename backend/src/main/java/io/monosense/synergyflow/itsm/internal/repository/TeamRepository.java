package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Team}.
 */
public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findByEnabledTrue();
}
