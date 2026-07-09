package com.scheduler.scheduling.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "day_plan_items")
@Getter
@Setter
@NoArgsConstructor
public class DayPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_plan_id", nullable = false)
    private DayPlan dayPlan;

    private Long taskId;

    private String occurrenceKey;

    @Column(nullable = false)
    private String titleSnapshot;

    private String categorySnapshot;

    private String taskTypeSnapshot;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayPlanItemStatus status = DayPlanItemStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayPlanActionSource actionSource = DayPlanActionSource.GENERATED;

    @Column(length = 2000)
    private String notes;

    private Integer prioritySnapshot;

    private String recurrencePatternSnapshot;

    private Long addressIdSnapshot;

    private String addressTextSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpStatus followUpStatus = FollowUpStatus.NOT_NEEDED;

    private LocalDateTime followUpPromptedAt;

    private LocalDateTime followUpAnsweredAt;

    private String followUpAnswer;

    private Integer remainingMinutes;
}
