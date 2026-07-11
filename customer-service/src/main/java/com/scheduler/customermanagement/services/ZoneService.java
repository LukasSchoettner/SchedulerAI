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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        validateConfig(configName, startTime, endTime);
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
        normalizeDefinition(def);
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
        if (dto.getPrimaryCategory() != null) existing.setPrimaryCategory(dto.getPrimaryCategory());
        if (dto.getSecondaryCategories() != null) existing.setSecondaryCategories(dto.getSecondaryCategories());
        if (dto.getBehaviorMode() != null) existing.setBehaviorMode(dto.getBehaviorMode());
        if (dto.getTargetPlacementMode() != null) existing.setTargetPlacementMode(dto.getTargetPlacementMode());
        existing.setPriorityOverrideThreshold(dto.getPriorityOverrideThreshold());
        normalizeDefinition(existing);
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

    private Integer normalizePriorityOverrideThreshold(Integer threshold) {
        return threshold != null && threshold == 5 ? threshold : null;
    }

    private void normalizeDefinition(ZoneDefinition def) {
        validateDefinition(def);
        def.setPriorityOverrideThreshold(normalizePriorityOverrideThreshold(def.getPriorityOverrideThreshold()));
        def.setBehaviorMode(normalizeBehaviorMode(def.getBehaviorMode()));
        def.setTargetPlacementMode(normalizeTargetPlacementMode(def.getTargetPlacementMode()));
        def.setPrimaryCategory(blankToNull(def.getPrimaryCategory()));
        if (def.getSecondaryCategories() == null) {
            def.setSecondaryCategories(new LinkedHashSet<>());
        } else if (def.getPrimaryCategory() != null) {
            def.setSecondaryCategories(def.getSecondaryCategories().stream()
                    .filter(category -> !def.getPrimaryCategory().equals(category))
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        if (def.getAllowedCategories() == null) {
            def.setAllowedCategories(new LinkedHashSet<>());
        }
        if (def.getExcludedCategories() == null) {
            def.setExcludedCategories(new LinkedHashSet<>());
        }

        Set<String> derivedAllowed = new LinkedHashSet<>();
        if (def.getPrimaryCategory() != null) {
            derivedAllowed.add(def.getPrimaryCategory());
        }
        derivedAllowed.addAll(def.getSecondaryCategories());
        if (!derivedAllowed.isEmpty()) {
            def.setAllowedCategories(derivedAllowed);
        }
    }

    private String normalizeBehaviorMode(String behaviorMode) {
        if ("PREFERRED".equalsIgnoreCase(behaviorMode)) {
            return "PREFERRED";
        }
        return "STRICT";
    }

    private String normalizeTargetPlacementMode(String targetPlacementMode) {
        if ("PREFER_INSIDE_WINDOW".equalsIgnoreCase(targetPlacementMode)) {
            return "PREFER_INSIDE_WINDOW";
        }
        if ("KEEP_INSIDE_WINDOW".equalsIgnoreCase(targetPlacementMode)) {
            return "KEEP_INSIDE_WINDOW";
        }
        return "ALLOW_ELSEWHERE";
    }

    private void validateConfig(String name, LocalTime startTime, LocalTime endTime) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scheduling Profile name is required");
        }
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default flexible planning window is invalid");
        }
        if (java.time.Duration.between(startTime, endTime).toMinutes() < 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Default flexible planning window must be at least 1 hour");
        }
    }

    private void validateDefinition(ZoneDefinition def) {
        if (def.getTitle() == null || def.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planning Window name is required");
        }
        if (def.getDayMask() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one day");
        }
        if (def.getStartTime() == null || def.getEndTime() == null || !def.getStartTime().isBefore(def.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planning Window time range is invalid");
        }
        if (java.time.Duration.between(def.getStartTime(), def.getEndTime()).toMinutes() < 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planning Window must be at least 15 minutes");
        }
        if (blankToNull(def.getPrimaryCategory()) == null && !hasAllowedCategories(def)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Main focus is required");
        }
        String primary = blankToNull(def.getPrimaryCategory());
        if (primary != null && def.getSecondaryCategories() != null && def.getSecondaryCategories().contains(primary)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Main focus cannot also be listed as also allowed");
        }
        Integer threshold = def.getPriorityOverrideThreshold();
        if (threshold != null && threshold != 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Urgent override must be priority 5 or off");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean hasAllowedCategories(ZoneDefinition def) {
        return def.getAllowedCategories() != null && !def.getAllowedCategories().isEmpty();
    }
}
