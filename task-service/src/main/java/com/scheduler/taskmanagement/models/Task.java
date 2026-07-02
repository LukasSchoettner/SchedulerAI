package com.scheduler.taskmanagement.models;

//import com.scheduler.customermanagement.models.Customer;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "task_discriminator", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be blank")
    @Column(nullable = false)
    private String title;

    private int priority;  // or an enum if you prefer: LOW, MEDIUM, HIGH, etc.

    @NotNull
    @Column(nullable = false)
    private LocalDateTime dueDate;     // "deadline" for the task

    @Nullable
    @Column(nullable = true)
    private TaskType type; // FIXED, FLEXIBLE, RECURRING

    @NotNull
    @Column(nullable = false)
    private LocalDateTime reminderDate;  // remind customer at this time

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status; // PENDING, COMPLETED, DELAYED, etc.

    // If you want an enum, you could do @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private String category;  // e.g., "Health", "Work", "School", "Recreation", ...

    // Dependencies: tasks that must be completed before this one
    @ManyToMany
    @JoinTable(
            name = "task_dependencies",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_task_id")
    )
    private Set<Task> dependencies = new HashSet<>();

    // For recurring tasks (put "NONE" or null if it's not recurring)
    private String recurrencePattern; // e.g. "NONE", "DAILY", "MON,WED,FRI", "RRULE:..."

    @ManyToOne
    @JoinColumn(name = "parent_project_id")
    private ProjectTask parentProject;

    // OPTIONAL reference to an address in routing-service
    @Column(name = "address_id", nullable = true)
    private Long addressId;

    // Optional: snapshot of the address text, for quick display in UI
    @Column(name = "address_text", length = 500, nullable = true)
    private String addressText;

    // user
    @Column(name = "customer_id", nullable = false)
    private Long customerId;


    // Constructor (for convenience)
    public Task(String title, int priority, LocalDateTime dueDate, LocalDateTime reminderDate,
                TaskStatus status, String description, String category, String recurrencePattern) {
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
        this.reminderDate = reminderDate;
        this.status = status;
        this.description = description;
        this.category = category;
        this.recurrencePattern = recurrencePattern;
    }
}
