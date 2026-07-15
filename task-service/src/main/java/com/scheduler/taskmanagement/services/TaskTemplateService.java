package com.scheduler.taskmanagement.services;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.taskmanagement.dto.TaskTemplateInstantiateRequest;
import com.scheduler.taskmanagement.dto.TaskTemplateRequest;
import com.scheduler.taskmanagement.dto.TaskTemplateResponse;
import com.scheduler.taskmanagement.models.TaskTemplate;
import com.scheduler.taskmanagement.repositories.TaskTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TaskTemplateService {

    private final TaskTemplateRepository templateRepository;
    private final TaskService taskService;
    private final Clock clock;

    public TaskTemplateService(TaskTemplateRepository templateRepository, TaskService taskService) {
        this(templateRepository, taskService, Clock.systemDefaultZone());
    }

    TaskTemplateService(TaskTemplateRepository templateRepository, TaskService taskService, Clock clock) {
        this.templateRepository = templateRepository;
        this.taskService = taskService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public List<TaskTemplateResponse> list(Long customerId) {
        return templateRepository
                .findByCustomerIdAndArchivedFalseOrderByDisplayOrderAscLastUsedAtDescCreatedAtAsc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskTemplateResponse create(TaskTemplateRequest request, Long customerId) {
        TaskTemplate template = new TaskTemplate();
        template.setCustomerId(customerId);
        applyRequest(template, request, true);
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public TaskTemplateResponse update(Long id, TaskTemplateRequest request, Long customerId) {
        TaskTemplate template = findOwned(id, customerId);
        applyRequest(template, request, false);
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void archive(Long id, Long customerId) {
        TaskTemplate template = findOwned(id, customerId);
        template.setArchived(true);
        templateRepository.save(template);
    }

    @Transactional
    public TaskDTO instantiate(Long id, TaskTemplateInstantiateRequest request, Long customerId) {
        TaskTemplate template = findOwned(id, customerId);
        TaskDTO task = buildTask(template, request == null ? new TaskTemplateInstantiateRequest() : request);
        TaskDTO created = taskService.createTask(task, customerId);
        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsedAt(LocalDateTime.now(clock));
        templateRepository.save(template);
        return created;
    }

    TaskTemplateResponse toResponse(TaskTemplate template) {
        return TaskTemplateResponse.builder()
                .id(template.getId())
                .title(template.getTitle())
                .category(template.getCategory())
                .defaultType(template.getDefaultType())
                .defaultPriority(template.getDefaultPriority())
                .defaultEstimatedDurationMinutes(template.getDefaultEstimatedDurationMinutes())
                .defaultFixedDurationMinutes(template.getDefaultFixedDurationMinutes())
                .description(template.getDescription())
                .addressId(template.getAddressId())
                .addressText(template.getAddressText())
                .displayOrder(template.getDisplayOrder())
                .icon(template.getIcon())
                .usageCount(template.getUsageCount())
                .lastUsedAt(template.getLastUsedAt())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .archived(template.isArchived())
                .build();
    }

    private TaskTemplate findOwned(Long id, Long customerId) {
        return templateRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task template not found"));
    }

    private void applyRequest(TaskTemplate template, TaskTemplateRequest request, boolean creating) {
        if (request == null) request = new TaskTemplateRequest();
        if (creating || request.getTitle() != null) {
            String title = cleanText(request.getTitle());
            if (title == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template title is required");
            }
            template.setTitle(title);
        }
        if (creating || request.getCategory() != null) {
            template.setCategory(cleanText(request.getCategory()) != null ? cleanText(request.getCategory()) : "Work");
        }
        if (creating || request.getDefaultType() != null) {
            template.setDefaultType(supportedTemplateType(request.getDefaultType()));
        }
        if (creating || request.getDefaultPriority() != null) {
            template.setDefaultPriority(clamp(request.getDefaultPriority(), 1, 5, 3));
        }
        if (creating || request.getDefaultEstimatedDurationMinutes() != null) {
            template.setDefaultEstimatedDurationMinutes(positiveOrDefault(request.getDefaultEstimatedDurationMinutes(), 60));
        }
        if (creating || request.getDefaultFixedDurationMinutes() != null) {
            template.setDefaultFixedDurationMinutes(positiveOrDefault(request.getDefaultFixedDurationMinutes(), 60));
        }
        if (request.getDescription() != null || creating) {
            template.setDescription(cleanText(request.getDescription()));
        }
        if (request.getAddressId() != null || creating) {
            template.setAddressId(positiveAddressId(request.getAddressId()));
        }
        if (request.getAddressText() != null || creating) {
            template.setAddressText(cleanText(request.getAddressText()));
        }
        if (request.getDisplayOrder() != null || creating) {
            template.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        }
        if (request.getIcon() != null || creating) {
            template.setIcon(cleanText(request.getIcon()));
        }
    }

    private TaskDTO buildTask(TaskTemplate template, TaskTemplateInstantiateRequest request) {
        if (template.getDefaultType() == TaskType.FIXED) {
            return buildFixedTask(template, request);
        }
        return buildFlexibleTask(template, request);
    }

    private FlexibleTaskDTO buildFlexibleTask(TaskTemplate template, TaskTemplateInstantiateRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : LocalDate.now(clock);
        LocalDateTime due = dueDate.atTime(23, 59);
        LocalDateTime earliest = Boolean.TRUE.equals(request.getScheduleToday()) ? now : null;
        int duration = positiveOrDefault(request.getEstimatedDuration(), positiveOrDefault(template.getDefaultEstimatedDurationMinutes(), 60));

        return new FlexibleTaskDTO(
                null,
                template.getTitle(),
                TaskType.FLEXIBLE,
                request.getPriority() != null ? clamp(request.getPriority(), 1, 5, template.getDefaultPriority()) : template.getDefaultPriority(),
                due,
                due,
                TaskStatus.PENDING,
                "NONE",
                request.getDescription() != null ? cleanText(request.getDescription()) : template.getDescription(),
                template.getCategory(),
                overrideAddressId(request, template),
                overrideAddressText(request, template),
                duration,
                10,
                earliest,
                due,
                TaskNature.FIXED_ESTIMATE,
                Math.min(duration, 30),
                Math.max(duration, 30),
                false,
                false,
                0,
                0
        );
    }

    private FixedTaskDTO buildFixedTask(TaskTemplate template, TaskTemplateInstantiateRequest request) {
        LocalDateTime start = request.getFixedStartDateTime();
        LocalDateTime end = request.getFixedEndDateTime();
        if (start == null && request.getFixedDate() != null && request.getFixedStartTime() != null) {
            start = LocalDateTime.of(request.getFixedDate(), request.getFixedStartTime());
        }
        if (start != null && end == null) {
            int duration = positiveOrDefault(request.getFixedDurationMinutes(), positiveOrDefault(template.getDefaultFixedDurationMinutes(), 60));
            end = start.plusMinutes(duration);
        }
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fixed templates require a valid start and end time");
        }

        return new FixedTaskDTO(
                null,
                template.getTitle(),
                TaskType.FIXED,
                request.getPriority() != null ? clamp(request.getPriority(), 1, 5, template.getDefaultPriority()) : template.getDefaultPriority(),
                end,
                start,
                TaskStatus.PENDING,
                "NONE",
                request.getDescription() != null ? cleanText(request.getDescription()) : template.getDescription(),
                template.getCategory(),
                overrideAddressId(request, template),
                overrideAddressText(request, template),
                start,
                end
        );
    }

    private Long overrideAddressId(TaskTemplateInstantiateRequest request, TaskTemplate template) {
        return request.getAddressId() != null ? positiveAddressId(request.getAddressId()) : template.getAddressId();
    }

    private String overrideAddressText(TaskTemplateInstantiateRequest request, TaskTemplate template) {
        return request.getAddressText() != null ? cleanText(request.getAddressText()) : template.getAddressText();
    }

    private static String cleanText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Long positiveAddressId(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private static TaskType supportedTemplateType(TaskType value) {
        return value == TaskType.FIXED ? TaskType.FIXED : TaskType.FLEXIBLE;
    }

    private static int clamp(Integer value, int min, int max, int fallback) {
        int next = value != null ? value : fallback;
        return Math.max(min, Math.min(max, next));
    }

    private static int positiveOrDefault(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }
}
