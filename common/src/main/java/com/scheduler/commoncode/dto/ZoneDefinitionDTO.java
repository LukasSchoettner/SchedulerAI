package com.scheduler.commoncode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDefinitionDTO {
    private Long id;
    private String title;
    private int dayMask;
    private LocalTime startTime;
    private LocalTime endTime;
    private Set<String> allowedCategories;
    private Set<String> excludedCategories;
    private Integer priorityOverrideThreshold;
    private String primaryCategory;
    private Set<String> secondaryCategories;
    private String behaviorMode;
    private String targetPlacementMode;
}
