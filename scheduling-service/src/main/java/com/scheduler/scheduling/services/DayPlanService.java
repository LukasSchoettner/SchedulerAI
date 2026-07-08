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
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.repositories.DayPlanRepository;
import com.scheduler.taskmanagement.grpc.TaskCreate;
import com.scheduler.taskmanagement.grpc.TaskServiceGrpc;
import com.scheduler.taskmanagement.grpc.UpdateTaskStatusRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
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

    @GrpcClient("task-service")
    private TaskServiceGrpc.TaskServiceBlockingStub taskStub;

    public DayPlanService(DayPlanRepository dayPlanRepository, TaskSchedulerService taskSchedulerService) {
        this.dayPlanRepository = dayPlanRepository;
        this.taskSchedulerService = taskSchedulerService;
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

        List<DayPlanItem> generatedItems = generateItems(customerId, date, skippedTaskIds, startAfter, durationOverrides);
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
        return toResponse(dayPlanRepository.save(plan));
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
        refreshPlanSummary(plan);
        return toResponse(dayPlanRepository.save(plan));
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
        return generatePlan(customerId, plan.getPlanDate(), startAfter);
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

    private List<DayPlanItem> generateItems(
            Long customerId,
            LocalDate date,
            Set<Long> skippedTaskIds,
            LocalDateTime startAfter,
            Map<Long, Integer> durationOverrides
    ) {
        try {
            return taskSchedulerService.scheduleTasksForCustomer(customerId, skippedTaskIds, startAfter, durationOverrides)
                    .getScheduledTasks()
                    .stream()
                    .flatMap(scheduledTask -> toItems(scheduledTask, date).stream())
                    .sorted(Comparator.comparing(DayPlanItem::getStartDateTime))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return new ArrayList<>();
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
                item.getRecurrencePatternSnapshot()
        );
    }
}
