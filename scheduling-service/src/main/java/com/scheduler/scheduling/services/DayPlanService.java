package com.scheduler.scheduling.services;

import com.google.protobuf.Timestamp;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.dto.DayPlanItemResponse;
import com.scheduler.scheduling.dto.DayPlanResponse;
import com.scheduler.scheduling.models.DayPlan;
import com.scheduler.scheduling.models.DayPlanActionSource;
import com.scheduler.scheduling.models.DayPlanItem;
import com.scheduler.scheduling.models.DayPlanItemStatus;
import com.scheduler.scheduling.models.DayPlanStatus;
import com.scheduler.scheduling.models.FollowUpStatus;
import com.scheduler.scheduling.models.Schedule;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.models.UnscheduledTaskReport;
import com.scheduler.scheduling.notifications.NotificationService;
import com.scheduler.scheduling.notifications.NotificationType;
import com.scheduler.scheduling.repositories.DayPlanRepository;
import com.scheduler.taskmanagement.grpc.TaskCreate;
import com.scheduler.taskmanagement.grpc.TaskServiceGrpc;
import com.scheduler.taskmanagement.grpc.UpdateTaskStatusRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DayPlanService {

    private final DayPlanRepository dayPlanRepository;
    private final TaskSchedulerService taskSchedulerService;
    private final NotificationService notificationService;

    @GrpcClient("task-service")
    private TaskServiceGrpc.TaskServiceBlockingStub taskStub;

    public DayPlanService(DayPlanRepository dayPlanRepository, TaskSchedulerService taskSchedulerService) {
        this(dayPlanRepository, taskSchedulerService, null);
    }

    @Autowired
    public DayPlanService(
            DayPlanRepository dayPlanRepository,
            TaskSchedulerService taskSchedulerService,
            NotificationService notificationService
    ) {
        this.dayPlanRepository = dayPlanRepository;
        this.taskSchedulerService = taskSchedulerService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public DayPlanResponse getPlan(Long customerId, LocalDate date) {
        DayPlan plan = dayPlanRepository.findByCustomerIdAndPlanDate(customerId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No day plan for date"));
        return toResponse(plan);
    }

    @Transactional
    public DayPlanResponse generatePlan(Long customerId, LocalDate date) {
        return generatePlan(customerId, date, null);
    }

    @Transactional
    public DayPlanResponse generatePlan(Long customerId, LocalDate date, LocalDateTime startAfter) {
        return generatePlan(customerId, date, startAfter, Map.of());
    }

    @Transactional
    public DayPlanResponse generatePlan(
            Long customerId,
            LocalDate date,
            LocalDateTime startAfter,
            Map<Long, Integer> durationOverrides
    ) {
        DayPlan plan = dayPlanRepository.findByCustomerIdAndPlanDate(customerId, date)
                .orElseGet(() -> {
                    DayPlan created = new DayPlan();
                    created.setCustomerId(customerId);
                    created.setPlanDate(date);
                    return created;
                });

        String confirmedSignature = plan.getStatus() == DayPlanStatus.CONFIRMED ? signatureFor(activeItems(plan)) : null;
        Set<Long> skippedTaskIds = plan.getItems().stream()
                .filter(item -> item.getStatus() == DayPlanItemStatus.SKIPPED)
                .map(DayPlanItem::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<DayPlanItem> preservedSkippedItems = plan.getItems().stream()
                .filter(item -> item.getStatus() == DayPlanItemStatus.SKIPPED)
                .map(this::copyDetached)
                .collect(Collectors.toCollection(ArrayList::new));

        GeneratedDayPlanData generatedData = generateItems(customerId, date, skippedTaskIds, startAfter, durationOverrides);
        List<DayPlanItem> generatedItems = generatedData.items();
        generatedItems.addAll(preservedSkippedItems);
        generatedItems.sort(Comparator.comparing(DayPlanItem::getStartDateTime).thenComparing(item -> item.getId() == null ? 0L : item.getId()));

        String nextSignature = signatureFor(generatedItems.stream()
                .filter(item -> item.getStatus() != DayPlanItemStatus.SKIPPED)
                .toList());

        plan.setGeneratedAt(LocalDateTime.now());
        plan.setStatus(DayPlanStatus.GENERATED);
        plan.setChangedFromConfirmed(confirmedSignature != null && !confirmedSignature.equals(nextSignature));
        plan.replaceItems(generatedItems);
        refreshPlanSummary(plan);
        DayPlan saved = dayPlanRepository.save(plan);
        createGenerationNotifications(saved, generatedData.unscheduledTasks());
        return toResponse(saved);
    }

    @Transactional
    public DayPlanResponse confirm(Long customerId, Long planId) {
        DayPlan plan = requireOwnedPlan(customerId, planId);
        plan.setStatus(DayPlanStatus.CONFIRMED);
        plan.setConfirmedAt(LocalDateTime.now());
        plan.setChangedFromConfirmed(false);
        plan.getItems().stream()
                .filter(item -> item.getStatus() == DayPlanItemStatus.PLANNED)
                .forEach(item -> {
                    item.setStatus(DayPlanItemStatus.KEPT);
                    item.setActionSource(DayPlanActionSource.USER_CONFIRMED);
                });
        plan.getItems().stream()
                .filter(this::isActiveFlexibleItem)
                .forEach(item -> {
                    item.setFollowUpStatus(FollowUpStatus.PENDING);
                    item.setFollowUpPromptedAt(null);
                    item.setFollowUpAnsweredAt(null);
                    item.setFollowUpAnswer(null);
                });
        refreshPlanSummary(plan);
        DayPlan saved = dayPlanRepository.save(plan);
        createConfirmationNotifications(saved);
        return toResponse(saved);
    }

    @Transactional
    public DayPlanResponse skipToday(Long customerId, Long planId, Long itemId) {
        DayPlan plan = requireOwnedPlan(customerId, planId);
        DayPlanItem item = requireItem(plan, itemId);
        item.setStatus(DayPlanItemStatus.SKIPPED);
        item.setActionSource(DayPlanActionSource.USER_MODIFIED);
        item.setNotes("Skipped for " + plan.getPlanDate());
        refreshPlanSummary(plan);
        return toResponse(dayPlanRepository.save(plan));
    }

    @Transactional
    public DayPlanResponse complete(Long customerId, Long planId, Long itemId) {
        DayPlan plan = requireOwnedPlan(customerId, planId);
        DayPlanItem item = requireItem(plan, itemId);
        item.setStatus(DayPlanItemStatus.COMPLETED);
        item.setActionSource(DayPlanActionSource.USER_MODIFIED);
        if (!TaskType.FIXED.name().equalsIgnoreCase(item.getTaskTypeSnapshot())) {
            item.setFollowUpStatus(FollowUpStatus.ANSWERED);
            item.setFollowUpAnsweredAt(LocalDateTime.now());
            item.setFollowUpAnswer("FINISHED");
            dismissUnreadForItem(customerId, item.getId());
        }

        if (item.getTaskId() != null && !hasRecurrence(item)) {
            taskStub.updateTaskStatus(UpdateTaskStatusRequest.newBuilder()
                    .setId(item.getTaskId())
                    .setCustomerId(customerId)
                    .setStatus(com.scheduler.taskmanagement.grpc.TaskStatus.COMPLETED)
                    .build());
        }

        refreshPlanSummary(plan);
        return toResponse(dayPlanRepository.save(plan));
    }

    @Transactional
    public DayPlanResponse keepFree(Long customerId, Long planId, Long itemId) {
        DayPlan plan = requireOwnedPlan(customerId, planId);
        DayPlanItem item = requireItem(plan, itemId);

        var created = taskStub.createTask(TaskCreate.newBuilder()
                .setCustomerId(customerId)
                .setTitle("Free time")
                .setType(com.scheduler.taskmanagement.grpc.TaskType.FIXED)
                .setPriority(5)
                .setDueDate(toTimestamp(item.getEndDateTime()))
                .setReminderDate(toTimestamp(item.getStartDateTime()))
                .setStatus(com.scheduler.taskmanagement.grpc.TaskStatus.PENDING)
                .setStartDateTime(toTimestamp(item.getStartDateTime()))
                .setEndDateTime(toTimestamp(item.getEndDateTime()))
                .setRecurrencePattern("NONE")
                .setDescription("Reserved from daily briefing")
                .setCategory("Leisure")
                .build());

        item.setTaskId(created.getId());
        item.setTitleSnapshot("Free time");
        item.setCategorySnapshot("Leisure");
        item.setTaskTypeSnapshot(TaskType.FIXED.name());
        item.setPrioritySnapshot(5);
        item.setRecurrencePatternSnapshot("NONE");
        item.setStatus(DayPlanItemStatus.FREE_TIME);
        item.setActionSource(DayPlanActionSource.USER_MODIFIED);
        item.setNotes("Reserved from daily briefing");
        item.setFollowUpStatus(FollowUpStatus.NOT_NEEDED);
        refreshPlanSummary(plan);
        return toResponse(dayPlanRepository.save(plan));
    }

    @Transactional
    public DayPlanResponse regenerate(Long customerId, Long planId) {
        return regenerate(customerId, planId, null);
    }

    @Transactional
    public DayPlanResponse regenerate(Long customerId, Long planId, LocalDateTime startAfter) {
        DayPlan plan = requireOwnedPlan(customerId, planId);
        DayPlanResponse response = generatePlan(customerId, plan.getPlanDate(), startAfter);
        createPlanChangedNotification(customerId, response.id());
        dismissFutureItemNotifications(customerId, response);
        return response;
    }

    @Transactional
    public DayPlanResponse rescheduleFlexibleItem(
            Long customerId,
            Long planId,
            Long itemId,
            LocalDateTime startAfter,
            String reason,
            Integer remainingMinutes
    ) {
        DayPlan plan = requireOwnedPlan(customerId, planId);
        DayPlanItem item = requireItem(plan, itemId);
        if (TaskType.FIXED.name().equalsIgnoreCase(item.getTaskTypeSnapshot())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fixed tasks cannot be rescheduled from follow-up");
        }
        if (item.getStatus() == DayPlanItemStatus.COMPLETED
                || item.getStatus() == DayPlanItemStatus.SKIPPED
                || item.getStatus() == DayPlanItemStatus.FREE_TIME) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is no longer reschedulable");
        }

        item.setStatus(DayPlanItemStatus.REPLACED);
        item.setActionSource(DayPlanActionSource.USER_MODIFIED);
        item.setNotes("Rescheduled from follow-up" + (reason != null && !reason.isBlank() ? ": " + reason : "")
                + (remainingMinutes != null && remainingMinutes > 0 ? "; remaining " + remainingMinutes + " minutes" : ""));
        item.setFollowUpStatus(FollowUpStatus.RESCHEDULED);
        item.setFollowUpAnsweredAt(LocalDateTime.now());
        item.setFollowUpAnswer(reason);
        item.setRemainingMinutes(remainingMinutes);
        dismissUnreadForItem(customerId, item.getId());
        refreshPlanSummary(plan);
        dayPlanRepository.save(plan);

        LocalDateTime effectiveStart = startAfter != null ? startAfter : LocalDateTime.now();
        Map<Long, Integer> durationOverrides = new HashMap<>();
        if (item.getTaskId() != null && remainingMinutes != null && remainingMinutes > 0) {
            durationOverrides.put(item.getTaskId(), remainingMinutes);
        }
        return generatePlan(customerId, plan.getPlanDate(), effectiveStart, durationOverrides);
    }

    private DayPlan requireOwnedPlan(Long customerId, Long planId) {
        DayPlan plan = dayPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Day plan not found"));
        if (!Objects.equals(plan.getCustomerId(), customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Day plan does not belong to customer");
        }
        return plan;
    }

    private DayPlanItem requireItem(DayPlan plan, Long itemId) {
        return plan.getItems().stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Day plan item not found"));
    }

    private GeneratedDayPlanData generateItems(
            Long customerId,
            LocalDate date,
            Set<Long> skippedTaskIds,
            LocalDateTime startAfter,
            Map<Long, Integer> durationOverrides
    ) {
        try {
            Schedule schedule = taskSchedulerService.scheduleTasksForCustomer(customerId, skippedTaskIds, startAfter, durationOverrides);
            List<DayPlanItem> items = schedule
                    .getScheduledTasks()
                    .stream()
                    .flatMap(scheduledTask -> toItems(scheduledTask, date).stream())
                    .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                    .collect(Collectors.toCollection(ArrayList::new));
            List<UnscheduledTaskReport> unscheduled = schedule.getUnscheduledTasks() != null
                    ? schedule.getUnscheduledTasks()
                    : List.of();
            return new GeneratedDayPlanData(items, unscheduled);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return new GeneratedDayPlanData(new ArrayList<>(), List.of());
            }
            throw e;
        }
    }

    private List<DayPlanItem> toItems(ScheduledTask scheduledTask, LocalDate date) {
        if (scheduledTask == null || scheduledTask.getTask() == null || scheduledTask.getAssignedSlots() == null) {
            return List.of();
        }

        return scheduledTask.getAssignedSlots().stream()
                .filter(slot -> slot.getStart() != null && slot.getStart().toLocalDate().equals(date))
                .map(slot -> toItem(scheduledTask, slot, date))
                .toList();
    }

    private DayPlanItem toItem(ScheduledTask scheduledTask, TimeSlot slot, LocalDate date) {
        var task = scheduledTask.getTask();
        DayPlanItem item = new DayPlanItem();
        item.setTaskId(task.getId());
        item.setOccurrenceKey(hasRecurrence(task.getRecurrencePattern()) && task.getId() != null
                ? task.getId() + ":" + date
                : null);
        item.setTitleSnapshot(task.getTitle());
        item.setCategorySnapshot(task.getCategory());
        item.setTaskTypeSnapshot(task.getType() != null ? task.getType().name() : null);
        item.setStartDateTime(slot.getStart());
        item.setEndDateTime(slot.getEnd());
        item.setStatus("Free time".equalsIgnoreCase(task.getTitle()) ? DayPlanItemStatus.FREE_TIME : DayPlanItemStatus.PLANNED);
        item.setActionSource(DayPlanActionSource.GENERATED);
        item.setPrioritySnapshot(task.getPriority());
        item.setRecurrencePatternSnapshot(task.getRecurrencePattern());
        item.setFollowUpStatus(task.getType() == TaskType.FLEXIBLE ? FollowUpStatus.NOT_NEEDED : FollowUpStatus.NOT_NEEDED);
        return item;
    }

    private DayPlanItem copyDetached(DayPlanItem source) {
        DayPlanItem copy = new DayPlanItem();
        copy.setTaskId(source.getTaskId());
        copy.setOccurrenceKey(source.getOccurrenceKey());
        copy.setTitleSnapshot(source.getTitleSnapshot());
        copy.setCategorySnapshot(source.getCategorySnapshot());
        copy.setTaskTypeSnapshot(source.getTaskTypeSnapshot());
        copy.setStartDateTime(source.getStartDateTime());
        copy.setEndDateTime(source.getEndDateTime());
        copy.setStatus(source.getStatus());
        copy.setActionSource(source.getActionSource());
        copy.setNotes(source.getNotes());
        copy.setPrioritySnapshot(source.getPrioritySnapshot());
        copy.setRecurrencePatternSnapshot(source.getRecurrencePatternSnapshot());
        copy.setFollowUpStatus(source.getFollowUpStatus());
        copy.setFollowUpPromptedAt(source.getFollowUpPromptedAt());
        copy.setFollowUpAnsweredAt(source.getFollowUpAnsweredAt());
        copy.setFollowUpAnswer(source.getFollowUpAnswer());
        copy.setRemainingMinutes(source.getRemainingMinutes());
        return copy;
    }

    private List<DayPlanItem> activeItems(DayPlan plan) {
        return plan.getItems().stream()
                .filter(item -> item.getStatus() != DayPlanItemStatus.SKIPPED)
                .filter(item -> item.getStatus() != DayPlanItemStatus.REPLACED)
                .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                .toList();
    }

    private String signatureFor(List<DayPlanItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                .map(item -> String.join("|",
                        String.valueOf(item.getTaskId()),
                        String.valueOf(item.getStartDateTime()),
                        String.valueOf(item.getEndDateTime()),
                        String.valueOf(item.getTitleSnapshot()),
                        String.valueOf(item.getCategorySnapshot()),
                        String.valueOf(item.getTaskTypeSnapshot())))
                .collect(Collectors.joining("::"));
    }

    private void refreshPlanSummary(DayPlan plan) {
        List<DayPlanItem> active = activeItems(plan);
        plan.setPlanSignature(signatureFor(active));
        plan.setFreeGapMinutes(freeMinutesBetween(plan.getItems()));
        plan.setTightSpotSummary(tightSpotSummary(plan.getItems()));
    }

    private Integer freeMinutesBetween(List<DayPlanItem> items) {
        List<DayPlanItem> active = items.stream()
                .filter(item -> item.getStatus() != DayPlanItemStatus.SKIPPED)
                .filter(item -> item.getStatus() != DayPlanItemStatus.REPLACED)
                .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                .toList();
        int minutes = 0;
        for (int i = 1; i < active.size(); i++) {
            DayPlanItem previous = active.get(i - 1);
            DayPlanItem next = active.get(i);
            if (previous.getEndDateTime().toLocalDate().equals(next.getStartDateTime().toLocalDate())) {
                long gap = java.time.Duration.between(previous.getEndDateTime(), next.getStartDateTime()).toMinutes();
                if (gap > 0) {
                    minutes += (int) gap;
                }
            }
        }
        return minutes;
    }

    private String tightSpotSummary(List<DayPlanItem> items) {
        int tight = 0;
        List<DayPlanItem> active = items.stream()
                .filter(item -> item.getStatus() != DayPlanItemStatus.SKIPPED)
                .filter(item -> item.getStatus() != DayPlanItemStatus.REPLACED)
                .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                .toList();
        for (int i = 1; i < active.size(); i++) {
            long gap = java.time.Duration.between(active.get(i - 1).getEndDateTime(), active.get(i).getStartDateTime()).toMinutes();
            if (gap >= 0 && gap < 10) {
                tight++;
            }
        }
        return tight == 0 ? "No tight transitions" : tight + " tight transition" + (tight == 1 ? "" : "s");
    }

    private boolean hasRecurrence(DayPlanItem item) {
        return hasRecurrence(item.getRecurrencePatternSnapshot());
    }

    private boolean hasRecurrence(String recurrencePattern) {
        return recurrencePattern != null && !recurrencePattern.isBlank() && !"NONE".equalsIgnoreCase(recurrencePattern);
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.newBuilder()
                .setSeconds(value.atZone(ZoneId.systemDefault()).toEpochSecond())
                .setNanos(value.getNano())
                .build();
    }

    private DayPlanResponse toResponse(DayPlan plan) {
        return new DayPlanResponse(
                plan.getId(),
                plan.getCustomerId(),
                plan.getPlanDate(),
                plan.getStatus(),
                plan.getGeneratedAt(),
                plan.getConfirmedAt(),
                plan.getReviewedAt(),
                plan.getPlanSignature(),
                plan.getFreeGapMinutes(),
                plan.getTightSpotSummary(),
                plan.getChangedFromConfirmed(),
                plan.getItems().stream()
                        .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                        .map(this::toResponse)
                        .toList()
        );
    }

    private DayPlanItemResponse toResponse(DayPlanItem item) {
        return new DayPlanItemResponse(
                item.getId(),
                item.getTaskId(),
                item.getOccurrenceKey(),
                item.getTitleSnapshot(),
                item.getCategorySnapshot(),
                item.getTaskTypeSnapshot(),
                item.getStartDateTime(),
                item.getEndDateTime(),
                item.getStatus(),
                item.getActionSource(),
                item.getNotes(),
                item.getPrioritySnapshot(),
                item.getRecurrencePatternSnapshot(),
                item.getFollowUpStatus(),
                item.getFollowUpPromptedAt(),
                item.getFollowUpAnsweredAt(),
                item.getFollowUpAnswer(),
                item.getRemainingMinutes()
        );
    }

    private void createGenerationNotifications(DayPlan plan, List<UnscheduledTaskReport> unscheduledTasks) {
        if (notificationService == null || plan == null || plan.getId() == null) return;
        if (plan.getStatus() != DayPlanStatus.CONFIRMED) {
            notificationService.createIfNotExists(
                    plan.getCustomerId(),
                    NotificationType.DAY_PLAN_CONFIRMATION_NEEDED,
                    "Confirm your day plan",
                    "Review and confirm your day plan so reminders can work properly.",
                    null,
                    plan.getId(),
                    null,
                    null
            );
        }
        int unscheduledCount = unscheduledTasks == null ? 0 : unscheduledTasks.size();
        if (unscheduledCount > 0) {
            notificationService.createIfNotExists(
                    plan.getCustomerId(),
                    NotificationType.UNSCHEDULED_TASKS,
                    "Some tasks could not be scheduled",
                    unscheduledCount + " tasks could not be placed today. Review them in the schedule.",
                    null,
                    plan.getId(),
                    null,
                    null
            );
        }
    }

    private void createConfirmationNotifications(DayPlan plan) {
        if (notificationService == null || plan == null || plan.getId() == null) return;
        activeItems(plan).forEach(item -> {
            notificationService.createTaskStartingSoon(plan.getCustomerId(), plan.getId(), item);
            if (isActiveFlexibleItem(item)) {
                notificationService.createFollowUpDue(plan.getCustomerId(), plan.getId(), item);
            }
        });
    }

    private void createPlanChangedNotification(Long customerId, Long planId) {
        if (notificationService == null) return;
        notificationService.createIfNotExists(
                customerId,
                NotificationType.PLAN_CHANGED,
                "Plan updated",
                "Your day plan was updated.",
                null,
                planId,
                null,
                null
        );
    }

    private void dismissFutureItemNotifications(Long customerId, DayPlanResponse response) {
        if (notificationService == null || response == null) return;
        notificationService.dismissFutureItemNotifications(
                customerId,
                response.items().stream().map(DayPlanItemResponse::id).filter(Objects::nonNull).toList()
        );
    }

    private void dismissUnreadForItem(Long customerId, Long itemId) {
        if (notificationService != null) {
            notificationService.dismissUnreadForItem(customerId, itemId);
        }
    }

    private boolean isActiveFlexibleItem(DayPlanItem item) {
        return item != null
                && !TaskType.FIXED.name().equalsIgnoreCase(item.getTaskTypeSnapshot())
                && item.getStatus() != DayPlanItemStatus.SKIPPED
                && item.getStatus() != DayPlanItemStatus.REPLACED
                && item.getStatus() != DayPlanItemStatus.FREE_TIME;
    }

    private record GeneratedDayPlanData(List<DayPlanItem> items, List<UnscheduledTaskReport> unscheduledTasks) {
    }
}
