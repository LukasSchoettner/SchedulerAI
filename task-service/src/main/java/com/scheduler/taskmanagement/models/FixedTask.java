package com.scheduler.taskmanagement.models;

import com.scheduler.commoncode.enums.TaskStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("FIXED")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FixedTask extends Task {

    private LocalDateTime startDateTime; // Required start
    private LocalDateTime endDateTime;   // Required end

    public FixedTask(String title,
                     int priority,
                     LocalDateTime dueDate,
                     LocalDateTime reminderDate,
                     TaskStatus status,
                     String description,
                     String category,
                     String recurrencePattern,
                     LocalDateTime startDateTime,
                     LocalDateTime endDateTime) {
        super(title, priority, dueDate, reminderDate, status, description, category, recurrencePattern);
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }
}
