package com.scheduler.commoncode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingPreferenceDTO {
    private Long id;
    private String primaryPriority;
    private List<String> categoryPriorityOrder = new ArrayList<>();
    private Set<String> fixedCommitmentCategories = new HashSet<>();
    private String workFlexibility;
    private Set<String> healthConstraints = new HashSet<>();
    private String allocationMode;
    private Map<String, Integer> taskCountTargets = new HashMap<>();
    private Integer plannedHoursPerDayMinutes;
    private Map<String, Integer> timeBudgetTargets = new HashMap<>();
    private String fixedTimeBudgetMode;
    private Map<String, Boolean> fixedTimeCountsByCategory = new HashMap<>();
    private List<MinimumRequirementDTO> minimumRequirements = new ArrayList<>();
    private Integer pauseMinutes;
    private String pauseOverloadBehavior;
    private String planningFullness;
    private List<String> overloadReductionOrder = new ArrayList<>();
    private String temporaryMode;
    private LocalDate temporaryUntil;
}
