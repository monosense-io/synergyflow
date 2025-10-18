package io.monosense.synergyflow.incident.domain;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for Incident entity.
 *
 * <p>Provides CRUD operations without writing SQL.
 * Spring Modulith ensures this mapper only accesses the incident module's schema.
 */
@Mapper
public interface IncidentMapper extends BaseMapper<Incident> {
}
