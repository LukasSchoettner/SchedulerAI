// src/main/java/com/scheduler/commoncode/dto/ProjectTaskDTO.java
package com.scheduler.commoncode.dto;

import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for project tasks, which contain a list of sub-tasks.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ProjectTaskDTO extends TaskDTO {
    private List<TaskDTO> subTasks;

    @Builder
    public ProjectTaskDTO(Long id,
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
                          List<TaskDTO> subTasks) {
        super(id, title, type, priority, dueDate, reminderDate, status,
                recurrencePattern, description, category, addressId, addressText);
        this.subTasks = subTasks;
    }
}