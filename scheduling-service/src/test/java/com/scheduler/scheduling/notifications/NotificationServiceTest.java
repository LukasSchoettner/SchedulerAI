package com.scheduler.scheduling.notifications;

import com.scheduler.scheduling.models.DayPlanItem;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 8, 10, 5);
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(
            repository,
            Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE)
    );

    @Test
    void duplicateUnreadNotificationHandlesNullIdentityFields() {
        Notification existing = notification(1L, 123L, NotificationType.DAY_PLAN_CONFIRMATION_NEEDED, null, 10L, null, null);
        when(repository.findByCustomerIdAndStatusAndType(
                123L,
                NotificationStatus.UNREAD,
                NotificationType.DAY_PLAN_CONFIRMATION_NEEDED
        )).thenReturn(List.of(existing));

        Notification result = service.createIfNotExists(
                123L,
                NotificationType.DAY_PLAN_CONFIRMATION_NEEDED,
                "Confirm",
                "Confirm plan",
                null,
                10L,
                null,
                null
        );

        assertThat(result).isSameAs(existing);
    }

    @Test
    void dueReturnsOnlyUnreadNotificationsDueNow() {
        Notification due = notification(1L, 123L, NotificationType.TASK_STARTING_SOON, 44L, 10L, 100L, NOW.minusMinutes(1));
        when(repository.findByCustomerIdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                123L,
                NotificationStatus.UNREAD,
                NOW
        )).thenReturn(List.of(due));

        var result = service.due(123L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(NotificationType.TASK_STARTING_SOON);
    }

    @Test
    void followUpDueUsesDayPlanItemEndTimeAsDueAt() {
        DayPlanItem item = new DayPlanItem();
        item.setId(100L);
        item.setTaskId(44L);
        item.setTitleSnapshot("Project report");
        item.setStartDateTime(LocalDateTime.of(2026, 7, 8, 11, 0));
        item.setEndDateTime(LocalDateTime.of(2026, 7, 8, 12, 15));
        when(repository.findByCustomerIdAndStatusAndType(
                123L,
                NotificationStatus.UNREAD,
                NotificationType.FOLLOW_UP_DUE
        )).thenReturn(List.of());
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = service.createFollowUpDue(123L, 10L, item);

        assertThat(notification.getDueAt()).isEqualTo(item.getEndDateTime());
        assertThat(notification.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void markReadAndDismissUpdateOwnedNotification() {
        Notification notification = notification(1L, 123L, NotificationType.FOLLOW_UP_DUE, 44L, 10L, 100L, NOW);
        when(repository.findById(1L)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var read = service.markRead(123L, 1L);
        var dismissed = service.dismiss(123L, 1L);

        assertThat(read.status()).isEqualTo(NotificationStatus.READ);
        assertThat(read.readAt()).isEqualTo(NOW);
        assertThat(dismissed.status()).isEqualTo(NotificationStatus.DISMISSED);
    }

    @Test
    void readAllMarksUnreadNotificationsForCustomer() {
        Notification first = notification(1L, 123L, NotificationType.PLAN_CHANGED, null, 10L, null, null);
        Notification second = notification(2L, 123L, NotificationType.UNSCHEDULED_TASKS, null, 10L, null, null);
        when(repository.findByCustomerIdAndStatus(123L, NotificationStatus.UNREAD)).thenReturn(List.of(first, second));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.markAllRead(123L);

        assertThat(result).extracting(NotificationDTO::status).containsExactly(NotificationStatus.READ, NotificationStatus.READ);
        assertThat(first.getReadAt()).isEqualTo(NOW);
        assertThat(second.getReadAt()).isEqualTo(NOW);
    }

    @Test
    void listUsesStatusFilterAndLimit() {
        Notification notification = notification(1L, 123L, NotificationType.PLAN_CHANGED, null, 10L, null, null);
        when(repository.findByCustomerIdAndStatusOrderByCreatedAtDesc(eq(123L), eq(NotificationStatus.UNREAD), any(Pageable.class)))
                .thenReturn(List.of(notification));

        var result = service.list(123L, NotificationStatus.UNREAD, 5);

        assertThat(result).hasSize(1);
        verify(repository).findByCustomerIdAndStatusOrderByCreatedAtDesc(eq(123L), eq(NotificationStatus.UNREAD), any(Pageable.class));
    }

    private Notification notification(
            Long id,
            Long customerId,
            NotificationType type,
            Long taskId,
            Long dayPlanId,
            Long itemId,
            LocalDateTime dueAt
    ) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setCustomerId(customerId);
        notification.setType(type);
        notification.setTitle(type.name());
        notification.setMessage(type.name());
        notification.setRelatedTaskId(taskId);
        notification.setRelatedDayPlanId(dayPlanId);
        notification.setRelatedDayPlanItemId(itemId);
        notification.setDueAt(dueAt);
        notification.setCreatedAt(NOW);
        notification.setStatus(NotificationStatus.UNREAD);
        return notification;
    }
}
