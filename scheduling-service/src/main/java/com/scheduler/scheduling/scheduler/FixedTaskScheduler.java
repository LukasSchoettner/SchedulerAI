package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FixedTaskDTO;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;

import java.util.List;

public class FixedTaskScheduler implements SchedulingStrategy<FixedTaskDTO> {

    @Override
    public ScheduledTask schedule(FixedTaskDTO task, List<TimeSlot> availableSlots) {
        // For fixed tasks, we ignore availableSlots and use start/end directly
        TimeSlot slot = new TimeSlot(task.getStartDateTime(), task.getEndDateTime());
        return new ScheduledTask(task, slot);
    }
}
