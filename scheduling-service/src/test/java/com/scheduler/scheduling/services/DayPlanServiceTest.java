package com.scheduler.scheduling.services;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.models.DayPlan;
import com.scheduler.scheduling.models.DayPlanActionSource;
import com.scheduler.scheduling.models.DayPlanItem;
import com.scheduler.scheduling.models.DayPlanItemStatus;
import com.scheduler.scheduling.models.DayPlanStatus;
import com.scheduler.scheduling.models.FollowUpStatus;
import com.scheduler.scheduling.models.Schedule;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.models.UnscheduledReasonCode;
import com.scheduler.scheduling.models.UnscheduledTaskReport;
import com.scheduler.scheduling.notifications.NotificationService;
import com.scheduler.scheduling.notifications.NotificationType;
import com.scheduler.scheduling.repositories.DayPlanRepository;
import com.scheduler.scheduling.routing.TravelWarningCode;
import com.scheduler.taskmanagement.grpc.TaskCreate;
import com.scheduler.taskmanagement.grpc.TaskProto;
import com.scheduler.taskmanagement.grpc.TaskServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

class DayPlanServiceTest {

    private final DayPlanRepository dayPlanRepository = mock(DayPlanRepository.class);
    private final TaskSchedulerService taskSchedulerService = mock(TaskSchedulerService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TaskServiceGrpc.TaskServiceBlockingStub taskStub = mock(TaskServiceGrpc.TaskServiceBlockingStub.class);
    private final DayPlanService service = new DayPlanService(dayPlanRepository, taskSchedulerService, notificationService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "taskStub", taskStub);
        when(dayPlanRepository.save(any(DayPlan.class))).thenAnswer(invocation -> {
            DayPlan plan = invocation.getArgument(0);
            if (plan.getId() == null) {
                plan.setId(10L);
            }
            long nextItemId = 100L;
            for (DayPlanItem item : plan.getItems()) {
                if (item.getId() == null) {
                    item.setId(nextItemId++);
                }
            }
            return plan;
        });
    }

    @Test
    void generatedDayPlanIsPersistedChronologically() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.empty());
        FlexibleTaskDTO later = flexibleTask(2L, "Later", "Duty");
        later.setAddressId(2L);
        later.setAddressText("Gym");
        FlexibleTaskDTO earlier = flexibleTask(1L, "Earlier", "Health");
        earlier.setAddressId(1L);
        earlier.setAddressText("Home");
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), any(), any())).thenReturn(schedule(
                later,
                slot(date.atTime(14, 0), date.atTime(15, 0)),
                earlier,
                slot(date.atTime(8, 0), date.atTime(8, 30))
        ));

        var response = service.generatePlan(123L, date);

        assertThat(response.status()).isEqualTo(DayPlanStatus.GENERATED);
        assertThat(response.items()).extracting("titleSnapshot").containsExactly("Earlier", "Later");
        assertThat(response.items().getFirst().addressIdSnapshot()).isEqualTo(1L);
        assertThat(response.items().getFirst().addressTextSnapshot()).isEqualTo("Home");
        assertThat(response.transitions()).hasSize(1);
        assertThat(response.transitions().getFirst().warningCode()).isEqualTo(TravelWarningCode.FEASIBLE);
        assertThat(response.planSignature()).contains("Earlier").contains("Later");
        verify(dayPlanRepository).save(any(DayPlan.class));
        verify(notificationService).createIfNotExists(
                eq(123L),
                eq(NotificationType.DAY_PLAN_CONFIRMATION_NEEDED),
                any(),
                any(),
                eq(null),
                eq(10L),
                eq(null),
                eq(null)
        );
    }

    @Test
    void confirmMarksPlannedItemsAsKeptAndStoresTimestamp() {
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.PLANNED);
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));

        var response = service.confirm(123L, 10L);

        assertThat(response.status()).isEqualTo(DayPlanStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
        assertThat(response.items().getFirst().status()).isEqualTo(DayPlanItemStatus.KEPT);
        assertThat(response.items().getFirst().actionSource()).isEqualTo(DayPlanActionSource.USER_CONFIRMED);
        assertThat(response.items().getFirst().followUpStatus()).isEqualTo(FollowUpStatus.PENDING);
        assertThat(response.transitions()).isEmpty();
        verify(notificationService).createTaskStartingSoon(eq(123L), eq(10L), any(DayPlanItem.class));
        verify(notificationService).createFollowUpDue(eq(123L), eq(10L), any(DayPlanItem.class));
    }

    @Test
    void skipTodayOnlyUpdatesTheDayPlanItem() {
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.PLANNED);
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));

        var response = service.skipToday(123L, 10L, 100L);

        assertThat(response.items().getFirst().status()).isEqualTo(DayPlanItemStatus.SKIPPED);
        assertThat(response.items().getFirst().notes()).contains("Skipped");
        verify(taskStub, never()).updateTaskStatus(any());
    }

    @Test
    void regenerationExcludesPersistedSkippedTaskIdsForSameDate() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.SKIPPED);
        plan.setPlanDate(date);
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.of(plan));
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), any(), any())).thenReturn(emptySchedule());

        service.regenerate(123L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> skippedCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(taskSchedulerService).scheduleTasksForCustomer(eq(123L), skippedCaptor.capture(), any(), any());
        assertThat(skippedCaptor.getValue()).containsExactly(44L);
    }

    @Test
    void regenerationDoesNotMarkUnchangedConfirmedPlanAsChanged() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.KEPT);
        plan.setPlanDate(date);
        plan.setStatus(DayPlanStatus.CONFIRMED);
        plan.setPlanSignature(String.join("|",
                "44",
                LocalDateTime.of(2026, 7, 4, 10, 0).toString(),
                LocalDateTime.of(2026, 7, 4, 10, 30).toString(),
                DayPlanItemStatus.KEPT.name(),
                "Grocery shopping",
                "old-format-extra-field"));
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.of(plan));
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), any(), any())).thenReturn(schedule(
                flexibleTask(44L, "Grocery shopping", "Duty"),
                slot(date.atTime(10, 0), date.atTime(10, 30)),
                flexibleTask(45L, "Ignored", "Health"),
                slot(date.plusDays(1).atTime(8, 0), date.plusDays(1).atTime(8, 30))
        ));

        var response = service.regenerate(123L, 10L);

        assertThat(response.changedFromConfirmed()).isFalse();
        assertThat(response.status()).isEqualTo(DayPlanStatus.GENERATED);
        assertThat(response.items()).extracting("status").containsExactly(DayPlanItemStatus.PLANNED);
        verify(notificationService).createIfNotExists(
                eq(123L),
                eq(NotificationType.PLAN_CHANGED),
                any(),
                any(),
                eq(null),
                eq(10L),
                eq(null),
                eq(null)
        );
    }

    @Test
    void keepFreeCreatesFixedFreeTimeTaskForCustomer() {
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.PLANNED);
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(taskStub.createTask(any(TaskCreate.class))).thenReturn(TaskProto.newBuilder().setId(99L).build());

        var response = service.keepFree(123L, 10L, 100L);

        ArgumentCaptor<TaskCreate> createCaptor = ArgumentCaptor.forClass(TaskCreate.class);
        verify(taskStub).createTask(createCaptor.capture());
        assertThat(createCaptor.getValue().getCustomerId()).isEqualTo(123L);
        assertThat(createCaptor.getValue().getTitle()).isEqualTo("Free time");
        assertThat(createCaptor.getValue().getType()).isEqualTo(com.scheduler.taskmanagement.grpc.TaskType.FIXED);
        assertThat(response.items().getFirst().status()).isEqualTo(DayPlanItemStatus.FREE_TIME);
        assertThat(response.items().getFirst().taskId()).isEqualTo(99L);
        assertThat(response.items().getFirst().followUpStatus()).isEqualTo(FollowUpStatus.NOT_NEEDED);
    }

    @Test
    void generatePassesEffectivePlanStartToScheduler() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        LocalDateTime startAfter = date.atTime(11, 15);
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.empty());
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), eq(startAfter), any())).thenReturn(emptySchedule());

        service.generatePlan(123L, date, startAfter);

        verify(taskSchedulerService).scheduleTasksForCustomer(eq(123L), any(Collection.class), eq(startAfter), any());
    }

    @Test
    void rescheduleWithRemainingMinutesPassesDurationOverride() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        LocalDateTime startAfter = date.atTime(11, 0);
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.PLANNED);
        plan.setPlanDate(date);
        DayPlanItem rescheduledItem = plan.getItems().getFirst();
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.of(plan));
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), eq(startAfter), any())).thenReturn(emptySchedule());

        service.rescheduleFlexibleItem(123L, 10L, 100L, startAfter, "STARTED_NOT_FINISHED", 25);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<Long, Integer>> overridesCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(taskSchedulerService).scheduleTasksForCustomer(eq(123L), any(Collection.class), eq(startAfter), overridesCaptor.capture());
        assertThat(overridesCaptor.getValue()).containsEntry(44L, 25);
        assertThat(rescheduledItem.getFollowUpStatus()).isEqualTo(FollowUpStatus.RESCHEDULED);
        assertThat(rescheduledItem.getFollowUpAnswer()).isEqualTo("STARTED_NOT_FINISHED");
        assertThat(rescheduledItem.getRemainingMinutes()).isEqualTo(25);
        verify(notificationService).dismissUnreadForItem(123L, 100L);
    }

    @Test
    void generateCreatesUnscheduledSummaryNotification() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.empty());
        Schedule schedule = emptySchedule();
        schedule.setUnscheduledTasks(List.of(new UnscheduledTaskReport(
                44L,
                "Too long",
                "Work",
                UnscheduledReasonCode.DURATION_TOO_LONG,
                "No slot"
        )));
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), any(), any())).thenReturn(schedule);

        service.generatePlan(123L, date);

        verify(notificationService).createIfNotExists(
                eq(123L),
                eq(NotificationType.UNSCHEDULED_TASKS),
                any(),
                any(),
                eq(null),
                eq(10L),
                eq(null),
                eq(null)
        );
    }

    @Test
    void completeUpdatesFollowUpStateAndClearsFollowUpNotification() {
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.KEPT);
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));

        var response = service.complete(123L, 10L, 100L);

        assertThat(response.items().getFirst().status()).isEqualTo(DayPlanItemStatus.COMPLETED);
        assertThat(response.items().getFirst().followUpStatus()).isEqualTo(FollowUpStatus.ANSWERED);
        assertThat(response.items().getFirst().followUpAnswer()).isEqualTo("FINISHED");
        verify(notificationService).dismissUnreadForItem(123L, 100L);
    }

    @Test
    void regenerateDismissesFutureNotificationsForInactiveItems() {
        LocalDate date = LocalDate.of(2026, 7, 4);
        DayPlan plan = planWithItem(123L, 10L, 100L, 44L, DayPlanItemStatus.KEPT);
        plan.setPlanDate(date);
        when(dayPlanRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(dayPlanRepository.findByCustomerIdAndPlanDate(123L, date)).thenReturn(Optional.of(plan));
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class), any(), any())).thenReturn(emptySchedule());

        service.regenerate(123L, 10L);

        verify(notificationService, atLeastOnce()).dismissFutureItemNotifications(eq(123L), any());
    }

    private DayPlan planWithItem(Long customerId, Long planId, Long itemId, Long taskId, DayPlanItemStatus status) {
        DayPlan plan = new DayPlan();
        plan.setId(planId);
        plan.setCustomerId(customerId);
        plan.setPlanDate(LocalDate.of(2026, 7, 4));
        plan.setGeneratedAt(LocalDateTime.of(2026, 7, 4, 7, 0));
        plan.setStatus(DayPlanStatus.GENERATED);

        DayPlanItem item = new DayPlanItem();
        item.setId(itemId);
        item.setTaskId(taskId);
        item.setTitleSnapshot("Grocery shopping");
        item.setCategorySnapshot("Duty");
        item.setTaskTypeSnapshot(TaskType.FLEXIBLE.name());
        item.setStartDateTime(LocalDateTime.of(2026, 7, 4, 10, 0));
        item.setEndDateTime(LocalDateTime.of(2026, 7, 4, 10, 30));
        item.setStatus(status);
        item.setActionSource(DayPlanActionSource.GENERATED);
        plan.addItem(item);
        return plan;
    }

    private Schedule schedule(FlexibleTaskDTO firstTask, TimeSlot firstSlot, FlexibleTaskDTO secondTask, TimeSlot secondSlot) {
        Schedule schedule = new Schedule();
        schedule.setScheduledTasks(new ArrayList<>(List.of(
                new ScheduledTask(firstTask, firstSlot),
                new ScheduledTask(secondTask, secondSlot)
        )));
        return schedule;
    }

    private Schedule emptySchedule() {
        Schedule schedule = new Schedule();
        schedule.setScheduledTasks(new ArrayList<>());
        return schedule;
    }

    private TimeSlot slot(LocalDateTime start, LocalDateTime end) {
        return new TimeSlot(start, end);
    }

    private FlexibleTaskDTO flexibleTask(Long id, String title, String category) {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setId(id);
        task.setTitle(title);
        task.setCategory(category);
        task.setType(TaskType.FLEXIBLE);
        task.setStatus(TaskStatus.PENDING);
        task.setTaskNature(TaskNature.FIXED_ESTIMATE);
        task.setPriority(3);
        task.setEstimatedDuration(30);
        task.setRecurrencePattern("NONE");
        return task;
    }
}
