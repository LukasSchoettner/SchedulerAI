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
import com.scheduler.scheduling.models.Schedule;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.repositories.DayPlanRepository;
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
import static org.mockito.Mockito.when;

class DayPlanServiceTest {

    private final DayPlanRepository dayPlanRepository = mock(DayPlanRepository.class);
    private final TaskSchedulerService taskSchedulerService = mock(TaskSchedulerService.class);
    private final TaskServiceGrpc.TaskServiceBlockingStub taskStub = mock(TaskServiceGrpc.TaskServiceBlockingStub.class);
    private final DayPlanService service = new DayPlanService(dayPlanRepository, taskSchedulerService);

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
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class))).thenReturn(schedule(
                flexibleTask(2L, "Later", "Duty"),
                slot(date.atTime(14, 0), date.atTime(15, 0)),
                flexibleTask(1L, "Earlier", "Health"),
                slot(date.atTime(8, 0), date.atTime(8, 30))
        ));

        var response = service.generatePlan(123L, date);

        assertThat(response.status()).isEqualTo(DayPlanStatus.GENERATED);
        assertThat(response.items()).extracting("titleSnapshot").containsExactly("Earlier", "Later");
        assertThat(response.planSignature()).contains("Earlier").contains("Later");
        verify(dayPlanRepository).save(any(DayPlan.class));
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
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class))).thenReturn(emptySchedule());

        service.regenerate(123L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> skippedCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(taskSchedulerService).scheduleTasksForCustomer(eq(123L), skippedCaptor.capture());
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
        when(taskSchedulerService.scheduleTasksForCustomer(eq(123L), any(Collection.class))).thenReturn(schedule(
                flexibleTask(44L, "Grocery shopping", "Duty"),
                slot(date.atTime(10, 0), date.atTime(10, 30)),
                flexibleTask(45L, "Ignored", "Health"),
                slot(date.plusDays(1).atTime(8, 0), date.plusDays(1).atTime(8, 30))
        ));

        var response = service.regenerate(123L, 10L);

        assertThat(response.changedFromConfirmed()).isFalse();
        assertThat(response.status()).isEqualTo(DayPlanStatus.GENERATED);
        assertThat(response.items()).extracting("status").containsExactly(DayPlanItemStatus.PLANNED);
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
