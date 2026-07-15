package com.scheduler.taskmanagement.dto;

import com.scheduler.commoncode.enums.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskTemplateResponse {
    private Long id;
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
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean archived;
}
