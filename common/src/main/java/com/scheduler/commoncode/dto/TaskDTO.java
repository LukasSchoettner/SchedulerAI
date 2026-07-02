// src/main/java/com/scheduler/commoncode/dto/TaskDTO.java
package com.scheduler.commoncode.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Base DTO for all task types. Uses Jackson polymorphic annotations to dispatch on 'type'.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FixedTaskDTO.class,    name = "FIXED"),
        @JsonSubTypes.Type(value = FlexibleTaskDTO.class, name = "FLEXIBLE"),
        @JsonSubTypes.Type(value = ProjectTaskDTO.class,  name = "PROJECT")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class TaskDTO {
    private Long id;
    private String title;
    private TaskType type;
    private Integer priority;
    private LocalDateTime dueDate;
    private LocalDateTime reminderDate;
    private TaskStatus status;
    private String recurrencePattern;
    private String description;
    private String category;
    private Long addressId;
    private String addressText;
}