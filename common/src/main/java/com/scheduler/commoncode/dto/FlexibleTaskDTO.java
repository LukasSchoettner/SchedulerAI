// src/main/java/com/scheduler/commoncode/dto/FlexibleTaskDTO.java
package com.scheduler.commoncode.dto;

import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for flexible tasks, supporting estimated durations, progressive scheduling, etc.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class FlexibleTaskDTO extends TaskDTO {
    private Integer estimatedDuration;
    private Integer bufferTime;
    private LocalDateTime earliestStartDateTime;
    private LocalDateTime latestEndDateTime;
    private TaskNature taskNature;
    private Integer minimalBlockSize;
    private Integer maximalBlockSize;
    private boolean canBeSeparated;
    private boolean progressive;
    private Integer cumulativeAllocatedTime = 0;
    private Integer targetAllocatedTime = 0;

    @Builder
    public FlexibleTaskDTO(Long id,
                           String title,
                           TaskType type,
                           int priority,
                           LocalDateTime dueDate,
                           LocalDateTime reminderDate,
                           TaskStatus status,
                           String recurrencePattern,
                           String description,
                           String category,
                           Long addressId,
                           String addressText,
                           Integer estimatedDuration,
                           Integer bufferTime,
                           LocalDateTime earliestStartDateTime,
                           LocalDateTime latestEndDateTime,
                           TaskNature taskNature,
                           int minimalBlockSize,
                           int maximalBlockSize,
                           boolean canBeSeparated,
                           boolean progressive,
                           int cumulativeAllocatedTime,
                           int targetAllocatedTime) {
        super(id, title, type, priority, dueDate, reminderDate, status,
                recurrencePattern, description, category, addressId, addressText);
        this.estimatedDuration        = estimatedDuration;
        this.bufferTime = bufferTime;
        this.earliestStartDateTime    = earliestStartDateTime;
        this.latestEndDateTime        = latestEndDateTime;
        this.taskNature               = taskNature;
        this.minimalBlockSize         = minimalBlockSize;
        this.maximalBlockSize         = maximalBlockSize;
        this.canBeSeparated           = canBeSeparated;
        this.progressive              = progressive;
        this.cumulativeAllocatedTime  = cumulativeAllocatedTime;
        this.targetAllocatedTime      = targetAllocatedTime;
    }
}