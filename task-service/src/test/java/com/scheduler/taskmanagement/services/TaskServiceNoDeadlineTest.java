package com.scheduler.taskmanagement.services;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.taskmanagement.models.FlexibleTask;
import com.scheduler.taskmanagement.models.FixedTask;
import com.scheduler.taskmanagement.repositories.TaskRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceNoDeadlineTest {

    private final TaskRepository repository = mock(TaskRepository.class);
    private final TaskService service = new TaskService(repository);

    @Test
    void createsFlexibleTaskWithoutSyntheticDeadlineReminderOrManualPriority() {
        when(repository.save(any(FlexibleTask.class))).thenAnswer(invocation -> {
            FlexibleTask task = invocation.getArgument(0);
            task.setId(10L);
            return task;
        });

        FlexibleTaskDTO created = (FlexibleTaskDTO) service.createTask(noDeadlineDto(), 7L);

        assertThat(created.getDueDate()).isNull();
        assertThat(created.getReminderDate()).isNull();
        assertThat(created.getLatestEndDateTime()).isNull();
        assertThat(created.getPriority()).isZero();
    }

    @Test
    void fixedTaskKeepsAuthoritativeStartAndEndWithoutSyntheticDeadline() {
        when(repository.save(any(FixedTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FixedTaskDTO dto = new FixedTaskDTO();
        dto.setTitle("Appointment");
        dto.setType(TaskType.FIXED);
        dto.setPriority(0);
        dto.setStatus(TaskStatus.PENDING);
        dto.setRecurrencePattern("NONE");
        dto.setStartDateTime(LocalDateTime.of(2026, 9, 22, 9, 30));
        dto.setEndDateTime(LocalDateTime.of(2026, 9, 22, 10, 15));

        FixedTaskDTO created = (FixedTaskDTO) service.createTask(dto, 7L);

        assertThat(created.getStartDateTime()).isEqualTo(LocalDateTime.of(2026, 9, 22, 9, 30));
        assertThat(created.getEndDateTime()).isEqualTo(LocalDateTime.of(2026, 9, 22, 10, 15));
        assertThat(created.getDueDate()).isNull();
    }

    @Test
    void omittedDeadlineAndPriorityPreserveStoredValues() {
        FlexibleTask existing = flexibleEntity();
        LocalDateTime due = existing.getDueDate();
        when(repository.findByIdAndCustomerId(11L, 7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FlexibleTaskDTO update = new FlexibleTaskDTO();
        update.setType(TaskType.FLEXIBLE);
        update.setTitle("Updated title");
        service.updateTaskFromRest(11L, update, 7L, Set.of("type", "title"));

        assertThat(existing.getDueDate()).isEqualTo(due);
        assertThat(existing.getPriority()).isEqualTo(4);
    }

    @Test
    void explicitNullDeadlineClearsDeadlineReminderAndLatestFinish() {
        FlexibleTask existing = flexibleEntity();
        when(repository.findByIdAndCustomerId(11L, 7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FlexibleTaskDTO update = new FlexibleTaskDTO();
        update.setType(TaskType.FLEXIBLE);
        update.setDueDate(null);
        update.setLatestEndDateTime(null);
        service.updateTaskFromRest(
                11L,
                update,
                7L,
                Set.of("type", "dueDate", "latestEndDateTime")
        );

        assertThat(existing.getDueDate()).isNull();
        assertThat(existing.getReminderDate()).isNull();
        assertThat(existing.getLatestEndDateTime()).isNull();
    }

    @Test
    void explicitZeroPriorityRestoresCategoryDerivedPriority() {
        FlexibleTask existing = flexibleEntity();
        when(repository.findByIdAndCustomerId(11L, 7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        FlexibleTaskDTO update = new FlexibleTaskDTO();
        update.setType(TaskType.FLEXIBLE);
        update.setPriority(0);
        service.updateTaskFromRest(11L, update, 7L, Set.of("type", "priority"));

        assertThat(existing.getPriority()).isZero();
    }

    private FlexibleTaskDTO noDeadlineDto() {
        FlexibleTaskDTO dto = new FlexibleTaskDTO();
        dto.setTitle("Someday task");
        dto.setType(TaskType.FLEXIBLE);
        dto.setPriority(null);
        dto.setStatus(TaskStatus.PENDING);
        dto.setRecurrencePattern("NONE");
        dto.setCategory("Work");
        dto.setEstimatedDuration(60);
        dto.setBufferTime(10);
        dto.setTaskNature(TaskNature.FIXED_ESTIMATE);
        dto.setMinimalBlockSize(30);
        dto.setMaximalBlockSize(120);
        return dto;
    }

    private FlexibleTask flexibleEntity() {
        FlexibleTask task = new FlexibleTask(
                "Task",
                4,
                LocalDateTime.of(2026, 9, 25, 20, 0),
                LocalDateTime.of(2026, 9, 25, 19, 0),
                TaskStatus.PENDING,
                null,
                "Work",
                "NONE",
                60,
                TaskNature.FIXED_ESTIMATE,
                30,
                120,
                false,
                false,
                10,
                LocalDateTime.of(2026, 9, 21, 12, 0),
                LocalDateTime.of(2026, 9, 25, 20, 0)
        );
        task.setId(11L);
        task.setCustomerId(7L);
        return task;
    }
}
