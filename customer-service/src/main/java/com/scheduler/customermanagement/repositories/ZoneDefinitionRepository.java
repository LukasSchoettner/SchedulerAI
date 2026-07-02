package com.scheduler.customermanagement.repositories;

import com.scheduler.customermanagement.models.ZoneDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneDefinitionRepository extends JpaRepository<ZoneDefinition, Long> {
    List<ZoneDefinition> findByZoneConfigId(Long configId);
    void deleteByZoneConfigId(Long zoneConfigId);
}

