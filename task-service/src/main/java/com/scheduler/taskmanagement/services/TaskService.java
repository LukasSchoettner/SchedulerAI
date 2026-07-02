// src/main/java/com/scheduler/taskmanagement/services/TaskService.java
package com.scheduler.taskmanagement.services;

import com.scheduler.commoncode.dto.*;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.taskmanagement.models.*;
import com.scheduler.taskmanagement.repositories.TaskRepository;
import com.scheduler.taskmanagement.util.TaskGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Creates a new Task entity from the polymorphic DTO and returns a DTO of the saved entity.
     */
    public TaskDTO createTask(TaskDTO dto, Long customerId) {
        Task entity;
        if (dto instanceof FixedTaskDTO fixedDto) {
            entity = createFixedTask(fixedDto);
        } else if (dto instanceof FlexibleTaskDTO flexDto) {
            entity = createFlexibleTask(flexDto);
        } else if (dto instanceof ProjectTaskDTO projDto) {
            entity = createProjectTask(projDto);
        } else {
            throw new IllegalArgumentException("Unknown TaskDTO subtype: " + dto.getClass());
        }
        entity.setCustomerId(customerId);
        Task saved = taskRepository.save(entity);
        return mapToDTO(saved);
    }

    //todo: delete due date from fixed task - unnecessary
    private FixedTask createFixedTask(FixedTaskDTO dto) {

        LocalDateTime now = LocalDateTime.now();
        // use dueDate as LocalDateTime if provided, else default to now
        LocalDateTime due = dto.getDueDate() != null ? dto.getDueDate() : now;
        // start = provided or now
        LocalDateTime start = dto.getStartDateTime() != null ? dto.getStartDateTime() : now;
        // end = provided or due
        LocalDateTime end = dto.getEndDateTime() != null ? dto.getEndDateTime() : due;
        // reminder = provided or midpoint(start,end)
        LocalDateTime reminder = dto.getReminderDate() != null
                ? dto.getReminderDate()
                : start.plus(Duration.between(start, end).dividedBy(2));

        FixedTask task = new FixedTask(
                dto.getTitle(),
                dto.getPriority(),
                due,
                reminder,
                dto.getStatus(),
                dto.getDescription(),
                dto.getCategory(),
                dto.getRecurrencePattern(),
                start,
                end
        );

        // NEW: copy routing info
        task.setAddressId(dto.getAddressId());
        task.setAddressText(dto.getAddressText());

        return task;
    }

    private FlexibleTask createFlexibleTask(FlexibleTaskDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = dto.getDueDate() != null ? dto.getDueDate() : now;
        LocalDateTime earliest = dto.getEarliestStartDateTime() != null
                ? dto.getEarliestStartDateTime() : now;
        LocalDateTime latest = dto.getLatestEndDateTime() != null
                ? dto.getLatestEndDateTime() : due;
        LocalDateTime reminder = dto.getReminderDate() != null
                ? dto.getReminderDate()
                : earliest.plus(Duration.between(earliest, latest).dividedBy(2));

        FlexibleTask task = new FlexibleTask(
                dto.getTitle(),
                dto.getPriority(),
                due,
                reminder,
                dto.getStatus(),
                dto.getDescription(),
                dto.getCategory(),
                dto.getRecurrencePattern(),
                dto.getEstimatedDuration(),
                dto.getTaskNature(),
                dto.getMinimalBlockSize(),
                dto.getMaximalBlockSize(),
                dto.isCanBeSeparated(),
                dto.isProgressive(),
                dto.getBufferTime(),
                earliest,
                latest
        );
        task.setCumulativeAllocatedTime(dto.getCumulativeAllocatedTime());
        task.setTargetAllocatedTime(dto.getTargetAllocatedTime());

        // NEW: copy routing info
        task.setAddressId(dto.getAddressId());
        task.setAddressText(dto.getAddressText());

        return task;
    }

    private ProjectTask createProjectTask(ProjectTaskDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = dto.getDueDate() != null ? dto.getDueDate() : now;
        // reminder halfway between now and due
        LocalDateTime reminder = dto.getReminderDate() != null
                ? dto.getReminderDate()
                : now.plus(Duration.between(now, due).dividedBy(2));

        ProjectTask project = new ProjectTask(
                dto.getTitle(),
                dto.getPriority(),
                due,
                reminder,
                dto.getStatus(),
                dto.getDescription(),
                dto.getCategory(),
                dto.getRecurrencePattern()
        );

        // NEW: copy routing info
        project.setAddressId(dto.getAddressId());
        project.setAddressText(dto.getAddressText());

        if (dto.getSubTasks() != null) {
            for (TaskDTO childDto : dto.getSubTasks()) {
                Task child = createTaskInternal(childDto);
                project.addSubTask(child);
            }
        }
        return project;
    }

    private Task createTaskInternal(TaskDTO dto) {
        if (dto instanceof FixedTaskDTO fixedDto) {
            return createFixedTask(fixedDto);
        } else if (dto instanceof FlexibleTaskDTO flexDto) {
            return createFlexibleTask(flexDto);
        } else if (dto instanceof ProjectTaskDTO projDto) {
            return createProjectTask(projDto);
        } else {
            throw new IllegalArgumentException("Unknown TaskDTO subtype");
        }
    }

    /**
     * Updates an existing Task and returns the updated DTO.
     */
    @Transactional
    public Optional<TaskDTO> updateTask(Long id, TaskDTO dto, Long customerId) {
        return taskRepository
                .findByIdAndCustomerId(id, customerId)
                .map(existing -> {
                    // --- update common fields if present ---
                    if (dto.getTitle() != null) {
                        existing.setTitle(dto.getTitle());
                    }
                    if (dto.getPriority() != null) {
                        existing.setPriority(dto.getPriority());
                    }
                    if (dto.getDueDate() != null) {
                        existing.setDueDate(dto.getDueDate());
                    }
                    if (dto.getReminderDate() != null) {
                        existing.setReminderDate(dto.getReminderDate());
                    }
                    if (dto.getStatus() != null) {
                        existing.setStatus(dto.getStatus());
                    }
                    if (dto.getDescription() != null) {
                        existing.setDescription(dto.getDescription());
                    }
                    if (dto.getCategory() != null) {
                        existing.setCategory(dto.getCategory());
                    }
                    if (dto.getRecurrencePattern() != null) {
                        existing.setRecurrencePattern(dto.getRecurrencePattern());
                    }
                    // NEW: address updates
                    if (dto.getAddressId() != null) {
                        existing.setAddressId(dto.getAddressId());
                    }
                    if (dto.getAddressText() != null) {
                        existing.setAddressText(dto.getAddressText());
                    }

                    // --- update subtype fields if present ---
                    if (existing instanceof FixedTask ft && dto instanceof FixedTaskDTO fixedDto) {
                        if (fixedDto.getStartDateTime() != null) {
                            ft.setStartDateTime(fixedDto.getStartDateTime());
                        }
                        if (fixedDto.getEndDateTime() != null) {
                            ft.setEndDateTime(fixedDto.getEndDateTime());
                        }

                    } else if (existing instanceof FlexibleTask fl && dto instanceof FlexibleTaskDTO flexDto) {
                        if (flexDto.getEstimatedDuration() != null) {
                            fl.setEstimatedDuration(flexDto.getEstimatedDuration());
                        }
                        if (flexDto.getBufferTime() != null) {
                            fl.setBufferTime(flexDto.getBufferTime());
                        }
                        if (flexDto.getTaskNature() != null) {
                            fl.setTaskNature(flexDto.getTaskNature());
                        }
                        if (flexDto.getMinimalBlockSize() != null) {
                            fl.setMinimalBlockSize(flexDto.getMinimalBlockSize());
                        }
                        if (flexDto.getMaximalBlockSize() != null) {
                            fl.setMaximalBlockSize(flexDto.getMaximalBlockSize());
                        }
                        // primitives like boolean: assume the DTO uses Boolean if you want null-check, otherwise always set
                        if (flexDto.isCanBeSeparated() != fl.isCanBeSeparated()) {
                            fl.setCanBeSeparated(flexDto.isCanBeSeparated());
                        }
                        if (flexDto.isProgressive() != fl.isProgressive()) {
                            fl.setProgressive(flexDto.isProgressive());
                        }
                        if (flexDto.getCumulativeAllocatedTime() != null) {
                            fl.setCumulativeAllocatedTime(flexDto.getCumulativeAllocatedTime());
                        }
                        if (flexDto.getTargetAllocatedTime() != null) {
                            fl.setTargetAllocatedTime(flexDto.getTargetAllocatedTime());
                        }
                        if (flexDto.getEarliestStartDateTime() != null) {
                            fl.setEarliestStartDateTime(flexDto.getEarliestStartDateTime());
                        }
                        if (flexDto.getLatestEndDateTime() != null) {
                            fl.setLatestEndDateTime(flexDto.getLatestEndDateTime());
                        }

                    } else if (existing instanceof ProjectTask pt && dto instanceof ProjectTaskDTO projDto) {
                        // (optional) merge sub-tasks here if projDto.getSubTasks() != null
                    }

                    // --- persist & map back to DTO ---
                    Task saved = taskRepository.save(existing);
                    return mapToDTO(saved);
                });
    }

    @Transactional
    public TaskDTO updateStatus(Long id, Long customerId, TaskStatus status) {
        System.out.println("updateStatus called with id=" + id + ", customerId=" + customerId + ", status=" + status);

        Task task = taskRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Task " + id + " not found for customer " + customerId
                ));

        task.setStatus(status);
        Task saved = taskRepository.save(task);
        return mapToDTO(saved);
    }

    public Optional<TaskDTO> getTaskById(Long id, Long customerId) {
        return taskRepository.findByIdAndCustomerId(id, customerId)
                .map(this::mapToDTO);
    }

    public List<TaskDTO> listTasksForCustomer(Long customerId) {
        return taskRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public boolean deleteTask(Long id, Long customerId) {
        if (taskRepository.existsByIdAndCustomerId(id, customerId)) {
            taskRepository.deleteByIdAndCustomerId(id, customerId);
            return true;
        }
        return false;
    }

    /**
     * Maps a Task entity to its corresponding DTO subtype.
     */
    private TaskDTO mapToDTO(Task task) {
        if (task instanceof FixedTask ft) {
            return new FixedTaskDTO(
                    ft.getId(),
                    ft.getTitle(),
                    TaskType.FIXED,
                    ft.getPriority(),
                    ft.getDueDate(),
                    ft.getReminderDate(),
                    ft.getStatus(),
                    ft.getRecurrencePattern(),
                    ft.getDescription(),
                    ft.getCategory(),
                    ft.getAddressId(),
                    ft.getAddressText(),
                    ft.getStartDateTime(),
                    ft.getEndDateTime()
            );
        } else if (task instanceof FlexibleTask fl) {
            return new FlexibleTaskDTO(
                    fl.getId(),
                    fl.getTitle(),
                    TaskType.FLEXIBLE,
                    fl.getPriority(),
                    fl.getDueDate(),
                    fl.getReminderDate(),
                    fl.getStatus(),
                    fl.getRecurrencePattern(),
                    fl.getDescription(),
                    fl.getCategory(),
                    fl.getAddressId(),
                    fl.getAddressText(),
                    fl.getEstimatedDuration(),
                    fl.getBufferTime(),
                    fl.getEarliestStartDateTime(),
                    fl.getLatestEndDateTime(),
                    fl.getTaskNature(),
                    fl.getMinimalBlockSize(),
                    fl.getMaximalBlockSize(),
                    fl.isCanBeSeparated(),
                    fl.isProgressive(),
                    fl.getCumulativeAllocatedTime(),
                    fl.getTargetAllocatedTime()
            );
        } else if (task instanceof ProjectTask pt) {
            List<TaskDTO> children = pt.getSubTasks().stream()
                    .map(this::mapToDTO)
                    .toList();
            return new ProjectTaskDTO(
                    pt.getId(),
                    pt.getTitle(),
                    TaskType.PROJECT,
                    pt.getPriority(),
                    pt.getDueDate(),
                    pt.getReminderDate(),
                    pt.getStatus(),
                    pt.getRecurrencePattern(),
                    pt.getDescription(),
                    pt.getCategory(),
                    pt.getAddressId(),
                    pt.getAddressText(),
                    children
            );
        } else {
            throw new IllegalArgumentException("Unknown Task subtype: " + task.getClass());
        }
    }

    public void generateDummyTasks(int count, Long customerId) {
        var dtos = TaskGenerator.generateTasks(count);
        for (TaskDTO dto : dtos) {
            // reuse your existing createTask
            createTask(dto, customerId);
        }
    }
}
