package com.scheduler.taskmanagement.services;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.taskmanagement.dto.TaskTemplateInstantiateRequest;
import com.scheduler.taskmanagement.dto.TaskTemplateRequest;
import com.scheduler.taskmanagement.models.TaskTemplate;
import com.scheduler.taskmanagement.repositories.TaskTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskTemplateServiceTest {

    private final TaskTemplateRepository repository = mock(TaskTemplateRepository.class);
    private final TaskService taskService = mock(TaskService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-15T08:00:00Z"), ZoneId.of("UTC"));
    private final TaskTemplateService service = new TaskTemplateService(repository, taskService, clock);

    @Test
    void newTemplateDefaultsUsageMetadataAndPreservesIconAndOrder() {
        TaskTemplateRequest request = new TaskTemplateRequest();
        request.setTitle("Buying groceries");
        request.setCategory("Duty");
        request.setDisplayOrder(4);
        request.setIcon("shopping_cart");
        when(repository.save(any(TaskTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request, 77L);

        assertThat(response.getUsageCount()).isZero();
        assertThat(response.getLastUsedAt()).isNull();
        assertThat(response.getDisplayOrder()).isEqualTo(4);
        assertThat(response.getIcon()).isEqualTo("shopping_cart");
    }

    @Test
    void listUsesRepositoryOrder() {
        when(repository.findByCustomerIdAndArchivedFalseOrderByDisplayOrderAscLastUsedAtDescCreatedAtAsc(77L))
                .thenReturn(List.of(template(1L, "First"), template(2L, "Second")));

        var result = service.list(77L);

        assertThat(result).extracting("title").containsExactly("First", "Second");
        verify(repository).findByCustomerIdAndArchivedFalseOrderByDisplayOrderAscLastUsedAtDescCreatedAtAsc(77L);
    }

    @Test
    void successfulFlexibleInstantiationReturnsCreatedTaskAndUpdatesUsage() {
        TaskTemplate template = template(1L, "Laundry");
        template.setCategory("Duty");
        template.setDefaultPriority(4);
        template.setDefaultEstimatedDurationMinutes(45);
        template.setDescription("Use gentle cycle");
        template.setAddressId(12L);
        template.setAddressText("Home");
        when(repository.findByIdAndCustomerId(1L, 77L)).thenReturn(Optional.of(template));
        when(taskService.createTask(any(TaskDTO.class), eq(77L))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskTemplateInstantiateRequest request = new TaskTemplateInstantiateRequest();
        request.setDueDate(LocalDate.of(2026, 7, 15));
        request.setScheduleToday(true);
        TaskDTO created = service.instantiate(1L, request, 77L);

        assertThat(created).isInstanceOf(FlexibleTaskDTO.class);
        FlexibleTaskDTO flexible = (FlexibleTaskDTO) created;
        assertThat(flexible.getTitle()).isEqualTo("Laundry");
        assertThat(flexible.getDueDate()).isEqualTo(LocalDateTime.of(2026, 7, 15, 23, 59));
        assertThat(flexible.getEarliestStartDateTime()).isEqualTo(LocalDateTime.of(2026, 7, 15, 8, 0));
        assertThat(flexible.getEstimatedDuration()).isEqualTo(45);
        assertThat(flexible.getAddressId()).isEqualTo(12L);
        assertThat(template.getUsageCount()).isEqualTo(1);
        assertThat(template.getLastUsedAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 8, 0));
        verify(repository).save(template);
    }

    @Test
    void failedFixedInstantiationDoesNotUpdateUsageMetadata() {
        TaskTemplate template = template(1L, "Doctor");
        template.setDefaultType(TaskType.FIXED);
        when(repository.findByIdAndCustomerId(1L, 77L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> service.instantiate(1L, new TaskTemplateInstantiateRequest(), 77L))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(template.getUsageCount()).isZero();
        assertThat(template.getLastUsedAt()).isNull();
        verify(taskService, never()).createTask(any(), any());
        verify(repository, never()).save(template);
    }

    @Test
    void fixedInstantiationWithTimingCreatesFixedTask() {
        TaskTemplate template = template(1L, "Doctor");
        template.setDefaultType(TaskType.FIXED);
        when(repository.findByIdAndCustomerId(1L, 77L)).thenReturn(Optional.of(template));
        when(taskService.createTask(any(TaskDTO.class), eq(77L))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskTemplateInstantiateRequest request = new TaskTemplateInstantiateRequest();
        request.setFixedDate(LocalDate.of(2026, 7, 15));
        request.setFixedStartTime(LocalTime.of(14, 30));
        request.setFixedDurationMinutes(30);

        TaskDTO created = service.instantiate(1L, request, 77L);

        assertThat(created).isInstanceOf(FixedTaskDTO.class);
        FixedTaskDTO fixed = (FixedTaskDTO) created;
        assertThat(fixed.getStartDateTime()).isEqualTo(LocalDateTime.of(2026, 7, 15, 14, 30));
        assertThat(fixed.getEndDateTime()).isEqualTo(LocalDateTime.of(2026, 7, 15, 15, 0));
        assertThat(template.getUsageCount()).isEqualTo(1);
    }

    @Test
    void updateRequestCannotOverwriteUsageMetadata() {
        TaskTemplate template = template(1L, "Old");
        template.setUsageCount(9);
        template.setLastUsedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        when(repository.findByIdAndCustomerId(1L, 77L)).thenReturn(Optional.of(template));
        when(repository.save(any(TaskTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskTemplateRequest request = new TaskTemplateRequest();
        request.setTitle("New");
        service.update(1L, request, 77L);

        assertThat(template.getUsageCount()).isEqualTo(9);
        assertThat(template.getLastUsedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
    }

    private TaskTemplate template(Long id, String title) {
        TaskTemplate template = new TaskTemplate();
        template.setId(id);
        template.setCustomerId(77L);
        template.setTitle(title);
        template.setCategory("Work");
        template.setDefaultType(TaskType.FLEXIBLE);
        template.setDefaultPriority(3);
        template.setDefaultEstimatedDurationMinutes(60);
        template.setDefaultFixedDurationMinutes(60);
        template.setDisplayOrder(0);
        template.setUsageCount(0);
        template.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 0).plusMinutes(id));
        template.setUpdatedAt(template.getCreatedAt());
        return template;
    }
}
