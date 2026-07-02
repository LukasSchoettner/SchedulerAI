// src/main/java/com/scheduler/commoncode/dto/FixedTaskDTO.java
package com.scheduler.commoncode.dto;

import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO for fixed tasks, with explicit start/end times.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class FixedTaskDTO extends TaskDTO {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @Builder
    public FixedTaskDTO(Long id,
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
                        LocalDateTime startDateTime,
                        LocalDateTime endDateTime) {
        super(id, title, type, priority, dueDate, reminderDate, status,
                recurrencePattern, description, category, addressId, addressText);
        this.startDateTime = startDateTime;
        this.endDateTime   = endDateTime;
    }
}