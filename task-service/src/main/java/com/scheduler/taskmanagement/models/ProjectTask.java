package com.scheduler.taskmanagement.models;

import com.scheduler.commoncode.enums.TaskStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("PROJECT")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProjectTask extends Task {

    // Sub-tasks can be either FixedTask, FlexibleTask, or even other ProjectTasks
    // Because everything is stored in the same table, we can reference them by ID
    // fetch type lazy to avoid loading entire projects by accident.
    @OneToMany(mappedBy = "parentProject", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Task> subTasks = new HashSet<>();

    // Example: maybe a project has a budget or other specialized fields
    // private BigDecimal budget;

    public ProjectTask(String title,
                       int priority,
                       LocalDateTime dueDate,
                       LocalDateTime reminderDate,
                       TaskStatus status,
                       String description,
                       String category,
                       String recurrencePattern) {
        super(title, priority, dueDate, reminderDate, status, description, category, recurrencePattern);
    }

    // Add convenience method to link sub-tasks
    public void addSubTask(Task subTask) {
        subTask.setParentProject(this);
        this.subTasks.add(subTask);
    }
}
