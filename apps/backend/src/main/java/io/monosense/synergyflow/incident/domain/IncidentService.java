package io.monosense.synergyflow.incident.domain;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Incident service interface (MyBatis-Plus pattern).
 */
public interface IncidentService extends IService<Incident> {

    /**
     * Create a new incident and publish IncidentCreatedEvent.
     *
     * @param incident Incident to create
     * @return Created incident with generated ID
     */
    Incident createIncident(Incident incident);
}
