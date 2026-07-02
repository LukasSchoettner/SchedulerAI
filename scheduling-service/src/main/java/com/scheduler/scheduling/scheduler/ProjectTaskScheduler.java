package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.strategy.SchedulingStrategy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ProjectTaskScheduler implements SchedulingStrategy<ProjectTaskDTO> {

    private static final int DEFAULT_PROJECT_MINUTES = 60;

    @Override
    public ScheduledTask schedule(ProjectTaskDTO task, List<TimeSlot> availableSlots) {
        if (task == null || availableSlots == null || availableSlots.isEmpty()) {
            return null;
        }

        int requiredMinutes = Math.max(computeRequiredMinutes(task), DEFAULT_PROJECT_MINUTES);
        for (TimeSlot slot : availableSlots) {
            LocalDateTime start = slot.getStart();
            LocalDateTime latestEnd = task.getDueDate() != null && task.getDueDate().isBefore(slot.getEnd())
                    ? task.getDueDate()
                    : slot.getEnd();
            LocalDateTime end = start.plusMinutes(requiredMinutes);

            if (!end.isAfter(latestEnd)) {
                return new ScheduledTask(task, new TimeSlot(start, end));
            }
        }

        return null;
    }

    private int computeRequiredMinutes(ProjectTaskDTO task) {
        if (task.getSubTasks() == null || task.getSubTasks().isEmpty()) {
            return 0;
        }

        return task.getSubTasks().stream()
                .mapToInt(this::computeTaskMinutes)
                .sum();
    }

    private int computeTaskMinutes(TaskDTO task) {
        if (task instanceof FlexibleTaskDTO flexibleTask) {
            Integer estimate = flexibleTask.getEstimatedDuration();
            return estimate != null && estimate > 0 ? estimate : 0;
        }

        if (task instanceof FixedTaskDTO fixedTask
                && fixedTask.getStartDateTime() != null
                && fixedTask.getEndDateTime() != null
                && fixedTask.getEndDateTime().isAfter(fixedTask.getStartDateTime())) {
            return (int) Duration.between(
                    fixedTask.getStartDateTime(),
                    fixedTask.getEndDateTime()
            ).toMinutes();
        }

        if (task instanceof ProjectTaskDTO projectTask) {
            return computeRequiredMinutes(projectTask);
        }

        return 0;
    }
}
