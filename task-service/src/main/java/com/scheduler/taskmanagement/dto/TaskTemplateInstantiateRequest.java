package com.scheduler.taskmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskTemplateInstantiateRequest {
    private LocalDate dueDate;
    private Boolean scheduleToday;
    private Integer estimatedDuration;
    private Integer priority;
    private String description;
    private Long addressId;
    private String addressText;
    private LocalDate fixedDate;
    private LocalTime fixedStartTime;
    private Integer fixedDurationMinutes;
    private LocalDateTime fixedStartDateTime;
    private LocalDateTime fixedEndDateTime;
}
