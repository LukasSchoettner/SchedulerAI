package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.scheduling.models.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.scheduler.scheduling.scheduler.GenericFlexibleTaskScheduler.MAX_BLOCK;
import static com.scheduler.scheduling.scheduler.GenericFlexibleTaskScheduler.MIN_BLOCK;

@Component("progressiveBlockSizer")
public class ProgressiveBlockSizer implements BlockSizer {

    @Override
    public int computeBlockSize(FlexibleTaskDTO task, TimeSlot slot, int remaining) {
        int minBlock = resolveMinBlock(task);
        int maxBlock = resolveMaxBlock(task);

        int slotMins = (int) slot.durationMinutes();
        if (slotMins < minBlock) {
            return 0; // too small to be useful
        }

        // If no dueDate: just give a middle-of-the-road block between min and max
        if (task.getDueDate() == null) {
            int mid = (minBlock + maxBlock) / 2;
            int desired = Math.min(remaining, Math.min(slotMins, mid));
            return Math.max(minBlock, desired);
        }

        long daysLeft = ChronoUnit.DAYS.between(
                java.time.LocalDate.now(),
                task.getDueDate().toLocalDate()
        );
        if (daysLeft < 0) {
            daysLeft = 0; // past due, treat as "very close"
        }

        // fraction in [0, 1]: 1 = far away, 0 = due now
        double fraction = Math.min(daysLeft / 30.0, 1.0);

        // Move gradually from minBlock (far away) to maxBlock (close to due)
        int progressive = (int) (maxBlock - ((maxBlock - minBlock) * fraction));

        int desired = Math.min(remaining, progressive);
        desired = Math.min(desired, slotMins); // cannot exceed slot length
        return Math.max(minBlock, desired);
    }

    private int resolveMinBlock(FlexibleTaskDTO task) {
        Integer m = task.getMinimalBlockSize();
        return (m != null && m > 0) ? m : 30;
    }

    private int resolveMaxBlock(FlexibleTaskDTO task) {
        Integer m = task.getMaximalBlockSize();
        int fallback = 120;
        if (m == null || m <= 0) return fallback;
        int min = resolveMinBlock(task);
        return Math.max(m, min);
    }
}

