// src/main/java/com/scheduler/customermanagement/services/ZoneService.java
package com.scheduler.customermanagement.services;

import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.customermanagement.mapper.ZoneConfigDtoMapper;
import com.scheduler.customermanagement.mapper.ZoneDefinitionDtoMapper;
import com.scheduler.customermanagement.models.ZoneConfiguration;
import com.scheduler.customermanagement.models.ZoneDefinition;
import com.scheduler.customermanagement.repositories.ZoneConfigurationRepository;
import com.scheduler.customermanagement.repositories.ZoneDefinitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ZoneService {

    private final ZoneConfigurationRepository configRepo;
    private final ZoneConfigDtoMapper mapper;
    private final ZoneDefinitionRepository defRepo;
    private final ZoneDefinitionDtoMapper defMapper;

    public ZoneService(ZoneConfigurationRepository configRepo,
                       ZoneConfigDtoMapper mapper,
                       ZoneDefinitionRepository defRepo,
                       ZoneDefinitionDtoMapper defMapper) {
        this.configRepo = configRepo;
        this.mapper = mapper;
        this.defRepo = defRepo;
        this.defMapper = defMapper;
    }

    public ZoneConfigurationDTO createZoneConfig(Long customerId, String configName, boolean active, LocalTime startTime, LocalTime endTime) {
        ZoneConfiguration config = new ZoneConfiguration();
        config.setCustomerId(customerId);
        config.setName(configName);
        config.setActive(active);
        config.setStartTime(startTime);
        config.setEndTime(endTime);
        if (active) {
            deactivateCustomerConfigs(customerId);
        }
        return mapper.toDto(configRepo.save(config));
    }

    public List<ZoneConfigurationDTO> listConfigs(Long customerId) {
        return configRepo.findAllByCustomerId(customerId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public ZoneConfigurationDTO activateConfig(Long customerId, Long configId) {
        ZoneConfiguration config = findOwnedConfig(customerId, configId);
        deactivateCustomerConfigs(customerId);
        config.setActive(true);

        return mapper.toDto(configRepo.save(config));
    }

    @Transactional(readOnly = true)
    public ZoneConfigurationDTO getActiveConfig(Long customerId) {
        ZoneConfiguration entity = configRepo.findByCustomerIdAndActiveTrue(customerId);
        return entity != null
                ? mapper.toDto(entity)
                : null;
    }

    @Transactional
    public boolean deleteZoneConfig(Long customerId, Long configId) {
        return configRepo.findById(configId)
                .filter(cfg -> cfg.getCustomerId().equals(customerId))
                .map(cfg -> {
                    defRepo.deleteByZoneConfigId(configId);
                    configRepo.delete(cfg);
                    return true;
                }).orElse(false);
    }

    public List<ZoneDefinitionDTO> listDefinitions(Long customerId, Long configId) {
        findOwnedConfig(customerId, configId);
        return defRepo.findByZoneConfigId(configId).stream()
                .map(defMapper::toDto)
                .toList();
    }

    public ZoneDefinitionDTO addDefinition(Long customerId,
                                           Long configId,
                                           ZoneDefinitionDTO dto) {
        findOwnedConfig(customerId, configId);
        ZoneDefinition def = defMapper.toDomain(dto);
        def.setId(null);
        def.setZoneConfigId(configId);
        return defMapper.toDto(defRepo.save(def));
    }

    public ZoneDefinitionDTO updateDefinition(Long customerId,
                                              Long configId,
                                              Long defId,
                                              ZoneDefinitionDTO dto) {
        findOwnedConfig(customerId, configId);
        ZoneDefinition existing = defRepo.findById(defId)
                .filter(d -> d.getZoneConfigId().equals(configId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone definition not found"));

        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        existing.setDayMask(dto.getDayMask());
        if (dto.getStartTime() != null) existing.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) existing.setEndTime(dto.getEndTime());
        if (dto.getAllowedCategories() != null) existing.setAllowedCategories(dto.getAllowedCategories());
        if (dto.getExcludedCategories() != null) existing.setExcludedCategories(dto.getExcludedCategories());
        if (dto.getPriorityOverrideThreshold() != null) {
            existing.setPriorityOverrideThreshold(dto.getPriorityOverrideThreshold());
        }
        return defMapper.toDto(defRepo.save(existing));
    }

    public boolean deleteDefinition(Long customerId, Long configId, Long defId) {
        findOwnedConfig(customerId, configId);
        return defRepo.findById(defId)
                .filter(d -> d.getZoneConfigId().equals(configId))
                .map(d -> {
                    defRepo.delete(d);
                    return true;
                })
                .orElse(false);
    }

    private ZoneConfiguration findOwnedConfig(Long customerId, Long configId) {
        return configRepo.findById(configId)
                .filter(cfg -> cfg.getCustomerId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone config not found"));
    }

    private void deactivateCustomerConfigs(Long customerId) {
        List<ZoneConfiguration> configs = configRepo.findAllByCustomerId(customerId);
        configs.forEach(cfg -> cfg.setActive(false));
        configRepo.saveAll(configs);
    }
}
