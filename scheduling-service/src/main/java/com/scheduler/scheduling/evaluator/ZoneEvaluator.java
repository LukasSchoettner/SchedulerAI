package com.scheduler.scheduling.evaluator;

import com.scheduler.commoncode.dto.TaskDTO;
import java.time.LocalDateTime;


public interface ZoneEvaluator {
    /**
     * Determines if a task fits within this zone for the given time slot.
     *
     * @param task the task being scheduled
     * @param slotStart the candidate slot start time
     * @param slotEnd the candidate slot end time
     * @return true if the task can be scheduled in this time slot based on zone rules
     */
    boolean isSatisfiedBy(TaskDTO task, LocalDateTime slotStart, LocalDateTime slotEnd);
}

