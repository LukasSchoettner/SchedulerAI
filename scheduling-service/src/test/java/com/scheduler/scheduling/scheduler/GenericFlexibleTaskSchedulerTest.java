package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericFlexibleTaskSchedulerTest {

    private final GenericFlexibleTaskScheduler scheduler = new GenericFlexibleTaskScheduler(
            new StandardBlockSizer(),
            new ProgressiveBlockSizer()
    );

    @Test
    void flexibleTaskDoesNotStartBeforeItsEarliestStart() {
        LocalDateTime day = LocalDateTime.now().plusDays(1).with(LocalTime.MIDNIGHT);
        FlexibleTaskDTO task = flexibleTask();
        task.setEarliestStartDateTime(day.with(LocalTime.of(8, 0)));
        task.setLatestEndDateTime(day.with(LocalTime.of(20, 0)));
        task.setDueDate(day.with(LocalTime.of(20, 0)));

        ScheduledTask scheduled = scheduler.schedule(task, List.of(
                new TimeSlot(day.with(LocalTime.MIDNIGHT), day.with(LocalTime.of(23, 59)))
        ));

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.getAssignedSlots())
                .allSatisfy(slot -> assertThat(slot.getStart().toLocalTime())
                        .isAfterOrEqualTo(LocalTime.of(8, 0)));
    }

    @Test
    void flexibleTaskDoesNotUseSlotsAfterLatestEnd() {
        LocalDateTime day = LocalDateTime.now().plusDays(1).with(LocalTime.MIDNIGHT);
        FlexibleTaskDTO task = flexibleTask();
        task.setEarliestStartDateTime(day.with(LocalTime.of(8, 0)));
        task.setLatestEndDateTime(day.with(LocalTime.of(10, 0)));
        task.setDueDate(day.with(LocalTime.of(10, 0)));

        ScheduledTask scheduled = scheduler.schedule(task, List.of(
                new TimeSlot(day.with(LocalTime.of(18, 0)), day.with(LocalTime.of(23, 0)))
        ));

        assertThat(scheduled).isNull();
    }

    private FlexibleTaskDTO flexibleTask() {
        FlexibleTaskDTO task = new FlexibleTaskDTO();
        task.setTitle("Prepare presentation");
        task.setType(TaskType.FLEXIBLE);
        task.setStatus(TaskStatus.PENDING);
        task.setCategory("Work");
        task.setPriority(3);
        task.setEstimatedDuration(60);
        task.setMinimalBlockSize(30);
        task.setMaximalBlockSize(60);
        task.setTaskNature(TaskNature.FIXED_ESTIMATE);
        task.setCanBeSeparated(false);
        task.setProgressive(false);
        task.setCumulativeAllocatedTime(0);
        task.setTargetAllocatedTime(0);
        return task;
    }
}
