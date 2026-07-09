package com.scheduler.scheduling.notifications;

import com.scheduler.scheduling.models.DayPlanItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, Clock.systemDefaultZone());
    }

    NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> list(Long customerId, NotificationStatus status, Integer limit) {
        PageRequest page = PageRequest.of(0, sanitizeLimit(limit));
        List<Notification> notifications = status == null
                ? notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, page)
                : notificationRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, status, page);
        return notifications.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> unread(Long customerId) {
        return list(customerId, NotificationStatus.UNREAD, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> due(Long customerId) {
        return notificationRepository
                .findByCustomerIdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                        customerId,
                        NotificationStatus.UNREAD,
                        LocalDateTime.now(clock)
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public NotificationDTO markRead(Long customerId, Long id) {
        Notification notification = requireOwned(customerId, id);
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now(clock));
        return toDto(notificationRepository.save(notification));
    }

    @Transactional
    public List<NotificationDTO> markAllRead(Long customerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Notification> unread = notificationRepository.findByCustomerIdAndStatus(customerId, NotificationStatus.UNREAD);
        unread.forEach(notification -> {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(now);
        });
        return notificationRepository.saveAll(unread).stream().map(this::toDto).toList();
    }

    @Transactional
    public NotificationDTO dismiss(Long customerId, Long id) {
        Notification notification = requireOwned(customerId, id);
        notification.setStatus(NotificationStatus.DISMISSED);
        return toDto(notificationRepository.save(notification));
    }

    @Transactional
    public Notification createIfNotExists(
            Long customerId,
            NotificationType type,
            String title,
            String message,
            Long relatedTaskId,
            Long relatedDayPlanId,
            Long relatedDayPlanItemId,
            LocalDateTime dueAt
    ) {
        return notificationRepository
                .findByCustomerIdAndStatusAndType(customerId, NotificationStatus.UNREAD, type)
                .stream()
                .filter(existing -> sameIdentity(existing, relatedTaskId, relatedDayPlanId, relatedDayPlanItemId))
                .findFirst()
                .orElseGet(() -> notificationRepository.save(newNotification(
                        customerId,
                        type,
                        title,
                        message,
                        relatedTaskId,
                        relatedDayPlanId,
                        relatedDayPlanItemId,
                        dueAt
                )));
    }

    @Transactional
    public void dismissUnreadForItem(Long customerId, Long dayPlanItemId) {
        if (dayPlanItemId == null) return;
        List<Notification> notifications = notificationRepository.findByCustomerIdAndRelatedDayPlanItemIdAndStatusIn(
                customerId,
                dayPlanItemId,
                List.of(NotificationStatus.UNREAD)
        );
        notifications.forEach(notification -> notification.setStatus(NotificationStatus.DISMISSED));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void dismissFutureItemNotifications(Long customerId, Collection<Long> activeItemIds) {
        List<Long> active = activeItemIds == null ? List.of() : activeItemIds.stream().filter(Objects::nonNull).toList();
        LocalDateTime now = LocalDateTime.now(clock);
        List<Notification> unread = notificationRepository.findByCustomerIdAndStatus(customerId, NotificationStatus.UNREAD);
        unread.stream()
                .filter(notification -> notification.getRelatedDayPlanItemId() != null)
                .filter(notification -> notification.getType() == NotificationType.TASK_STARTING_SOON
                        || notification.getType() == NotificationType.FOLLOW_UP_DUE)
                .filter(notification -> notification.getDueAt() == null || notification.getDueAt().isAfter(now))
                .filter(notification -> !active.contains(notification.getRelatedDayPlanItemId()))
                .forEach(notification -> notification.setStatus(NotificationStatus.DISMISSED));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void createTaskStartingSoon(Long customerId, Long dayPlanId, DayPlanItem item) {
        if (item == null || item.getStartDateTime() == null) return;
        LocalDateTime now = LocalDateTime.now(clock);
        if (!item.getStartDateTime().isAfter(now)) return;
        LocalDateTime dueAt = item.getStartDateTime().minusMinutes(10);
        if (dueAt.isBefore(now)) {
            dueAt = now;
        }
        createIfNotExists(
                customerId,
                NotificationType.TASK_STARTING_SOON,
                "Upcoming task",
                "\"" + item.getTitleSnapshot() + "\" starts at " + item.getStartDateTime().toLocalTime() + ".",
                item.getTaskId(),
                dayPlanId,
                item.getId(),
                dueAt
        );
    }

    @Transactional
    public void createFollowUpDue(Long customerId, Long dayPlanId, DayPlanItem item) {
        if (item == null || item.getEndDateTime() == null) return;
        createIfNotExists(
                customerId,
                NotificationType.FOLLOW_UP_DUE,
                "Task follow-up",
                "Did you finish \"" + item.getTitleSnapshot() + "\"?",
                item.getTaskId(),
                dayPlanId,
                item.getId(),
                item.getEndDateTime()
        );
    }

    public NotificationDTO toDto(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getCustomerId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedTaskId(),
                notification.getRelatedDayPlanId(),
                notification.getRelatedDayPlanItemId(),
                notification.getDueAt(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getStatus()
        );
    }

    private Notification requireOwned(Long customerId, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
        if (!Objects.equals(notification.getCustomerId(), customerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Notification does not belong to customer");
        }
        return notification;
    }

    private Notification newNotification(
            Long customerId,
            NotificationType type,
            String title,
            String message,
            Long relatedTaskId,
            Long relatedDayPlanId,
            Long relatedDayPlanItemId,
            LocalDateTime dueAt
    ) {
        Notification notification = new Notification();
        notification.setCustomerId(customerId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedTaskId(relatedTaskId);
        notification.setRelatedDayPlanId(relatedDayPlanId);
        notification.setRelatedDayPlanItemId(relatedDayPlanItemId);
        notification.setDueAt(dueAt);
        notification.setCreatedAt(LocalDateTime.now(clock));
        notification.setStatus(NotificationStatus.UNREAD);
        return notification;
    }

    private boolean sameIdentity(Notification notification, Long taskId, Long dayPlanId, Long dayPlanItemId) {
        return Objects.equals(notification.getRelatedTaskId(), taskId)
                && Objects.equals(notification.getRelatedDayPlanId(), dayPlanId)
                && Objects.equals(notification.getRelatedDayPlanItemId(), dayPlanItemId);
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}
