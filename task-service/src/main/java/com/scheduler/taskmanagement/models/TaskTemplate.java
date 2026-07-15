package com.scheduler.taskmanagement.models;

import com.scheduler.commoncode.enums.TaskType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_templates")
@Getter
@Setter
@NoArgsConstructor
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category = "Work";

    @Enumerated(EnumType.STRING)
    @Column(name = "default_type", nullable = false)
    private TaskType defaultType = TaskType.FLEXIBLE;

    @Column(name = "default_priority", nullable = false)
    private int defaultPriority = 3;

    @Column(name = "default_estimated_duration_minutes")
    private Integer defaultEstimatedDurationMinutes = 60;

    @Column(name = "default_fixed_duration_minutes")
    private Integer defaultFixedDurationMinutes = 60;

    @Column(length = 2000)
    private String description;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "address_text", length = 500)
    private String addressText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(length = 80)
    private String icon;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
