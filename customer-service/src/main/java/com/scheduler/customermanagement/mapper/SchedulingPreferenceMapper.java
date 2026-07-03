package com.scheduler.customermanagement.mapper;

import com.scheduler.commoncode.dto.MinimumRequirementDTO;
import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.customermanagement.models.MinimumRequirement;
import com.scheduler.customermanagement.models.SchedulingPreference;

import java.util.*;
import java.util.stream.Collectors;

public final class SchedulingPreferenceMapper {
    private static final List<String> DEFAULT_CATEGORY_PRIORITY = List.of(
            "Work", "Duty", "Health", "Social", "Sport", "Leisure"
    );

    private SchedulingPreferenceMapper() {
    }

    public static SchedulingPreferenceDTO toDto(SchedulingPreference entity) {
        if (entity == null) return null;

        SchedulingPreferenceDTO dto = new SchedulingPreferenceDTO();
        dto.setId(entity.getId());
        dto.setPrimaryPriority(entity.getPrimaryPriority());
        dto.setCategoryPriorityOrder(copyListOrDefault(entity.getCategoryPriorityOrder()));
        dto.setFixedCommitmentCategories(copySet(entity.getFixedCommitmentCategories()));
        dto.setWorkFlexibility(entity.getWorkFlexibility());
        dto.setHealthConstraints(copySet(entity.getHealthConstraints()));
        dto.setAllocationMode(entity.getAllocationMode());
        dto.setTaskCountTargets(copyMap(entity.getTaskCountTargets()));
        dto.setPlannedHoursPerDayMinutes(entity.getPlannedHoursPerDayMinutes());
        dto.setTimeBudgetTargets(copyMap(entity.getTimeBudgetTargets()));
        dto.setFixedTimeBudgetMode(entity.getFixedTimeBudgetMode());
        dto.setFixedTimeCountsByCategory(copyMap(entity.getFixedTimeCountsByCategory()));
        dto.setMinimumRequirements(entity.getMinimumRequirements() == null
                ? new ArrayList<>()
                : entity.getMinimumRequirements().stream()
                .map(SchedulingPreferenceMapper::toDto)
                .collect(Collectors.toList()));
        dto.setPauseMinutes(entity.getPauseMinutes());
        dto.setPauseOverloadBehavior(entity.getPauseOverloadBehavior());
        dto.setPlanningFullness(entity.getPlanningFullness());
        dto.setOverloadReductionOrder(entity.getOverloadReductionOrder() == null
                ? new ArrayList<>()
                : new ArrayList<>(entity.getOverloadReductionOrder()));
        dto.setTemporaryMode(entity.getTemporaryMode());
        dto.setTemporaryUntil(entity.getTemporaryUntil());
        return dto;
    }

    public static SchedulingPreference toEntity(SchedulingPreferenceDTO dto, Long customerId) {
        SchedulingPreference entity = new SchedulingPreference();
        applyToEntity(entity, dto, customerId);
        return entity;
    }

    public static void applyToEntity(SchedulingPreference entity, SchedulingPreferenceDTO dto, Long customerId) {
        entity.setCustomerId(customerId);
        entity.setPrimaryPriority(dto.getPrimaryPriority());
        entity.setCategoryPriorityOrder(copyListOrDefault(dto.getCategoryPriorityOrder()));
        entity.setFixedCommitmentCategories(copySet(dto.getFixedCommitmentCategories()));
        entity.setWorkFlexibility(dto.getWorkFlexibility());
        entity.setHealthConstraints(copySet(dto.getHealthConstraints()));
        entity.setAllocationMode(dto.getAllocationMode());
        entity.setTaskCountTargets(copyMap(dto.getTaskCountTargets()));
        entity.setPlannedHoursPerDayMinutes(dto.getPlannedHoursPerDayMinutes());
        entity.setTimeBudgetTargets(copyMap(dto.getTimeBudgetTargets()));
        entity.setFixedTimeBudgetMode(dto.getFixedTimeBudgetMode());
        entity.setFixedTimeCountsByCategory(copyMap(dto.getFixedTimeCountsByCategory()));
        entity.getMinimumRequirements().clear();
        if (dto.getMinimumRequirements() != null) {
            dto.getMinimumRequirements().stream()
                    .map(SchedulingPreferenceMapper::toEntity)
                    .forEach(entity.getMinimumRequirements()::add);
        }
        entity.setPauseMinutes(dto.getPauseMinutes());
        entity.setPauseOverloadBehavior(dto.getPauseOverloadBehavior());
        entity.setPlanningFullness(dto.getPlanningFullness());
        entity.setOverloadReductionOrder(dto.getOverloadReductionOrder() == null
                ? new ArrayList<>()
                : new ArrayList<>(dto.getOverloadReductionOrder()));
        entity.setTemporaryMode(dto.getTemporaryMode());
        entity.setTemporaryUntil(dto.getTemporaryUntil());
    }

    private static MinimumRequirementDTO toDto(MinimumRequirement entity) {
        return new MinimumRequirementDTO(
                entity.getCategory(),
                entity.getType(),
                entity.getAmount(),
                entity.getPeriod()
        );
    }

    private static MinimumRequirement toEntity(MinimumRequirementDTO dto) {
        MinimumRequirement entity = new MinimumRequirement();
        entity.setCategory(dto.getCategory());
        entity.setType(dto.getType());
        entity.setAmount(dto.getAmount());
        entity.setPeriod(dto.getPeriod());
        return entity;
    }

    private static Set<String> copySet(Set<String> value) {
        return value == null ? new HashSet<>() : new HashSet<>(value);
    }

    private static List<String> copyListOrDefault(List<String> value) {
        return value == null || value.isEmpty() ? new ArrayList<>(DEFAULT_CATEGORY_PRIORITY) : new ArrayList<>(value);
    }

    private static <T> Map<String, T> copyMap(Map<String, T> value) {
        return value == null ? new HashMap<>() : new HashMap<>(value);
    }
}
