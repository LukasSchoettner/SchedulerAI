package com.scheduler.scheduling.dto;

import com.scheduler.scheduling.models.DayPlanActionSource;
import com.scheduler.scheduling.models.DayPlanItemStatus;
import com.scheduler.scheduling.models.FollowUpStatus;

import java.time.LocalDateTime;

public record DayPlanItemResponse(
        Long id,
        Long taskId,
        String occurrenceKey,
        String titleSnapshot,
        String categorySnapshot,
        String taskTypeSnapshot,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        DayPlanItemStatus status,
        DayPlanActionSource actionSource,
        String notes,
        Integer prioritySnapshot,
        String recurrencePatternSnapshot,
        FollowUpStatus followUpStatus,
        LocalDateTime followUpPromptedAt,
        LocalDateTime followUpAnsweredAt,
        String followUpAnswer,
        Integer remainingMinutes
) {
}
