package com.scheduler.taskmanagement.models;

import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("FLEXIBLE")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FlexibleTask extends Task {

    private Integer estimatedDuration;   // in minutes, e.g. 90
    private Integer bufferTime;          // placeholder for transitions
    private TaskNature taskNature;       // FIXED_ESTIMATE, OPEN_ENDED
    private int minimalBlockSize = 0;
    private int maximalBlockSize = 0;
    private int cumulativeAllocatedTime;
    private int targetAllocatedTime = 0;
    private boolean canBeSeparated;
    private boolean progressive;

    // optional earliest / latest times
    private LocalDateTime earliestStartDateTime;
    private LocalDateTime latestEndDateTime;

    public FlexibleTask(String title,
                        int priority,
                        LocalDateTime dueDate,
                        LocalDateTime reminderDate,
                        TaskStatus status,
                        String description,
                        String category,
                        String recurrencePattern,
                        Integer estimatedDuration,
                        TaskNature taskNature,
                        int minimalBlockSize,
                        int maximalBlockSize,
                        boolean canBeSeparated,
                        boolean isProgressive,
                        Integer bufferTime,
                        LocalDateTime earliestStartDateTime,
                        LocalDateTime latestEndDateTime) {
        super(title, priority, dueDate, reminderDate, status, description, category, recurrencePattern);
        this.estimatedDuration = estimatedDuration;
        this.taskNature = taskNature;
        this.bufferTime = bufferTime;
        this.earliestStartDateTime = earliestStartDateTime;
        this.latestEndDateTime = latestEndDateTime;
        this.minimalBlockSize = minimalBlockSize;
        this.maximalBlockSize = maximalBlockSize;
        this.canBeSeparated = canBeSeparated;
        this.progressive = isProgressive;
    }
}
