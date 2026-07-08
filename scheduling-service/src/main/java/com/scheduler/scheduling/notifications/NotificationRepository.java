package com.scheduler.scheduling.notifications;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<Notification> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            Long customerId,
            NotificationStatus status,
            Pageable pageable
    );

    List<Notification> findByCustomerIdAndStatusAndType(
            Long customerId,
            NotificationStatus status,
            NotificationType type
    );

    List<Notification> findByCustomerIdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
            Long customerId,
            NotificationStatus status,
            LocalDateTime dueAt
    );

    List<Notification> findByCustomerIdAndStatus(Long customerId, NotificationStatus status);

    List<Notification> findByCustomerIdAndRelatedDayPlanItemIdAndStatusIn(
            Long customerId,
            Long relatedDayPlanItemId,
            List<NotificationStatus> statuses
    );
}
