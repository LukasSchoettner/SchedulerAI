package com.scheduler.taskmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scheduler.commoncode.enums.TaskType;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskTemplateRequest {
    private String title;
    private String category;
    private TaskType defaultType;
    private Integer defaultPriority;
    private Integer defaultEstimatedDurationMinutes;
    private Integer defaultFixedDurationMinutes;
    private String description;
    private Long addressId;
    private String addressText;
    private Integer displayOrder;
    private String icon;
}
