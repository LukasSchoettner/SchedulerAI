package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.dto.ProjectTaskDTO;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTaskSchedulerTest {

    private final ProjectTaskScheduler scheduler = new ProjectTaskScheduler();

    @Test
    void schedulesProjectIntoFirstSlotThatFitsSubtaskDuration() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 2, 9, 0);
        ProjectTaskDTO project = new ProjectTaskDTO();
        project.setTitle("Launch");
        project.setType(TaskType.PROJECT);
        project.setStatus(TaskStatus.PENDING);
        project.setSubTasks(List.of(
                flexibleTask(45),
                fixedTask(base, base.plusMinutes(30))
        ));

        ScheduledTask scheduled = scheduler.schedule(project, List.of(
                new TimeSlot(base, base.plusMinutes(60)),
                new TimeSlot(base.plusHours(2), base.plusHours(4))
        ));

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.getAssignedSlots()).hasSize(1);
        assertThat(scheduled.getAssignedSlots().get(0).getStart()).isEqualTo(base.plusHours(2));
        assertThat(scheduled.getAssignedSlots().get(0).getEnd()).isEqualTo(base.plusHours(3).plusMinutes(15));
    }

    @Test
    void usesDefaultDurationWhenProjectHasNoSubtasks() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 2, 9, 0);
        ProjectTaskDTO project = new ProjectTaskDTO();
        project.setTitle("Planning");
        project.setType(TaskType.PROJECT);

        ScheduledTask scheduled = scheduler.schedule(project, List.of(
                new TimeSlot(base, base.plusHours(2))
        ));

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.getAssignedSlots().get(0).getStart()).isEqualTo(base);
        assertThat(scheduled.getAssignedSlots().get(0).getEnd()).isEqualTo(base.plusHours(1));
    }

    @Test
    void returnsNullWhenNoSlotCanFitBeforeDueDate() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 2, 9, 0);
        ProjectTaskDTO project = new ProjectTaskDTO();
        project.setTitle("Launch");
        project.setType(TaskType.PROJECT);
        project.setDueDate(base.plusMinutes(30));

        ScheduledTask scheduled = scheduler.schedule(project, List.of(
                new TimeSlot(base, base.plusHours(2))
        ));

        assertThat(scheduled).isNull();
    }

    private FlexibleTaskDTO flexibleTask(int estimatedDuration) {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setType(TaskType.FLEXIBLE);
        task.setEstimatedDuration(estimatedDuration);
        return task;
    }

    private FixedTaskDTO fixedTask(LocalDateTime start, LocalDateTime end) {
        FixedTaskDTO task = new FixedTaskDTO();
        task.setType(TaskType.FIXED);
        task.setStartDateTime(start);
        task.setEndDateTime(end);
        return task;
    }
}
