package com.scheduler.taskmanagement.services;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.taskmanagement.models.FixedTask;
import com.scheduler.taskmanagement.models.FlexibleTask;
import com.scheduler.taskmanagement.repositories.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceRemainingDurationTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskService taskService = new TaskService(taskRepository);

    @Test
    void updatesExistingFlexibleTaskRemainingDuration() {
        FlexibleTask task = flexibleTask();
        task.setId(44L);
        task.setCustomerId(123L);
        task.setEstimatedDuration(60);
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findByIdAndCustomerId(44L, 123L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(FlexibleTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = taskService.updateFlexibleTaskRemainingDuration(
                44L,
                123L,
                25,
                LocalDateTime.of(2026, 7, 17, 11, 0),
                LocalDateTime.of(2026, 7, 17, 23, 59)
        );

        assertThat(result).isInstanceOf(FlexibleTaskDTO.class);
        FlexibleTaskDTO dto = (FlexibleTaskDTO) result;
        assertThat(dto.getId()).isEqualTo(44L);
        assertThat(dto.getEstimatedDuration()).isEqualTo(25);
        assertThat(dto.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(dto.getEarliestStartDateTime()).isEqualTo(LocalDateTime.of(2026, 7, 17, 11, 0));
        assertThat(dto.getDueDate()).isEqualTo(LocalDateTime.of(2026, 7, 17, 23, 59));
        verify(taskRepository).save(task);
    }

    @Test
    void updateIsIdempotentForSameRemainingDuration() {
        FlexibleTask task = flexibleTask();
        task.setId(44L);
        task.setCustomerId(123L);
        task.setEstimatedDuration(25);
        when(taskRepository.findByIdAndCustomerId(44L, 123L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(FlexibleTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.updateFlexibleTaskRemainingDuration(44L, 123L, 25, null, null);
        taskService.updateFlexibleTaskRemainingDuration(44L, 123L, 25, null, null);

        assertThat(task.getEstimatedDuration()).isEqualTo(25);
        verify(taskRepository, times(2)).save(task);
    }

    @Test
    void rejectsInvalidRemainingMinutes() {
        assertThatThrownBy(() -> taskService.updateFlexibleTaskRemainingDuration(44L, 123L, 0, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(taskRepository, never()).findByIdAndCustomerId(any(), any());
    }

    @Test
    void fixedTasksCannotUseRemainingDurationUpdate() {
        FixedTask fixedTask = new FixedTask();
        fixedTask.setId(44L);
        fixedTask.setCustomerId(123L);
        when(taskRepository.findByIdAndCustomerId(44L, 123L)).thenReturn(Optional.of(fixedTask));

        assertThatThrownBy(() -> taskService.updateFlexibleTaskRemainingDuration(44L, 123L, 25, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void customerOwnershipIsEnforcedByLookup() {
        when(taskRepository.findByIdAndCustomerId(44L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateFlexibleTaskRemainingDuration(44L, 999L, 25, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");

        verify(taskRepository, never()).save(any());
    }

    private FlexibleTask flexibleTask() {
        FlexibleTask task = new FlexibleTask(
                "Study",
                3,
                LocalDateTime.of(2026, 7, 17, 20, 0),
                LocalDateTime.of(2026, 7, 17, 19, 0),
                TaskStatus.PENDING,
                null,
                "Education",
                "NONE",
                60,
                TaskNature.FIXED_ESTIMATE,
                30,
                120,
                false,
                false,
                10,
                LocalDateTime.of(2026, 7, 17, 8, 0),
                LocalDateTime.of(2026, 7, 17, 20, 0)
        );
        return task;
    }
}
