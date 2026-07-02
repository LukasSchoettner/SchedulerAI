package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.scheduling.models.TimeSlot;

public interface BlockSizer {
    /**
     * @param task       the DTO, with estimatedDuration, targetAllocatedTime, cumulativeAllocatedTime, dueDate…
     * @param slot       the candidate slot
     * @param remaining  minutes still needed
     */
    int computeBlockSize(FlexibleTaskDTO task, TimeSlot slot, int remaining);
}

