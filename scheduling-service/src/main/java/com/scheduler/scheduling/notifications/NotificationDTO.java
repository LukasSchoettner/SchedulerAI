package com.scheduler.scheduling.notifications;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        Long customerId,
        NotificationType type,
        String title,
        String message,
        Long relatedTaskId,
        Long relatedDayPlanId,
        Long relatedDayPlanItemId,
        LocalDateTime dueAt,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        NotificationStatus status
) {
}
