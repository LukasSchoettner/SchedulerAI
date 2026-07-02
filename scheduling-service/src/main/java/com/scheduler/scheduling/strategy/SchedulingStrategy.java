package com.scheduler.scheduling.strategy;

import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.commoncode.dto.TaskDTO;

import java.util.List;

public interface SchedulingStrategy<T extends TaskDTO> {
    /**
     * Schedules the given task.
     *
     * @param task the task to schedule
     * @param availableSlots the candidate time slots (pre-computed from customer zones)
     * @return the scheduled task (with assigned time slot) or an error if scheduling fails.
     */
    ScheduledTask schedule(T task, List<TimeSlot> availableSlots);
}

