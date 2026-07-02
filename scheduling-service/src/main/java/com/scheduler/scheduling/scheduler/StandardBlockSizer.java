package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.scheduling.models.TimeSlot;
import org.springframework.stereotype.Component;

@Component("standardBlockSizer")
public class StandardBlockSizer implements BlockSizer {

    @Override
    public int computeBlockSize(FlexibleTaskDTO task, TimeSlot slot, int remaining) {
        int minBlock = resolveMinBlock(task);
        int maxBlock = resolveMaxBlock(task);

        int slotMins = (int) slot.durationMinutes();
        if (slotMins < minBlock) {
            // slot is too small to fit even the minimal block
            return 0;
        }

        // We never want to allocate more than:
        // - what remains for this task in this run
        // - the length of the slot
        // - the task's maximal block size
        int desired = Math.min(remaining, slotMins);
        desired = Math.min(desired, maxBlock);

        // Ensure we respect minimal block size where it makes sense
        if (desired < minBlock) {
            // Option A: still allocate minBlock (slight overshoot wrt 'remaining')
            // Option B: return 0 and leave this slot unused
            // I'll pick A, it's usually more practical:
            desired = minBlock;
        }

        return desired;
    }

    private int resolveMinBlock(FlexibleTaskDTO task) {
        Integer m = task.getMinimalBlockSize();
        return (m != null && m > 0) ? m : 30; // default 30
    }

    private int resolveMaxBlock(FlexibleTaskDTO task) {
        Integer m = task.getMaximalBlockSize();
        int fallback = 120; // default 120
        if (m == null || m <= 0) return fallback;
        int min = resolveMinBlock(task);
        return Math.max(m, min);
    }
}

