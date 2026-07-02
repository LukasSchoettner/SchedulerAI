package com.scheduler.scheduling.scheduler;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.scheduling.models.ScheduledTask;
import com.scheduler.scheduling.models.TimeSlot;
import com.scheduler.scheduling.strategy.SchedulingStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GenericFlexibleTaskScheduler implements SchedulingStrategy<FlexibleTaskDTO> {

    public static final int MIN_BLOCK = 30;
    public static final int MAX_BLOCK = 120;
    private final Map<Boolean, BlockSizer> sizers;

    public GenericFlexibleTaskScheduler(
            BlockSizer standardBlockSizer,
            BlockSizer progressiveBlockSizer
    ) {
        this.sizers = Map.of(
                false, standardBlockSizer,
                true, progressiveBlockSizer
        );
    }

    @Override
    public ScheduledTask schedule(FlexibleTaskDTO task, List<TimeSlot> slots) {
        int needed = computeTotalNeeded(task);
        List<TimeSlot> usedSlots = new ArrayList<>();
        BlockSizer sizer = sizers.getOrDefault(task.isProgressive(), sizers.get(false));

        for (TimeSlot slot : slots) {
            if (needed <= 0) break;
            int minutes = sizer.computeBlockSize(task, slot, needed);
            if (minutes <= 0) continue;
            TimeSlot assigned = new TimeSlot(
                    slot.getStart(),
                    slot.getStart().plusMinutes(minutes)
            );
            usedSlots.add(assigned);
            // advance the slot start
            slot.setStart(assigned.getEnd());
            needed -= minutes;
        }

        return usedSlots.isEmpty() ? null : new ScheduledTask(task, usedSlots);
    }

    private int computeTotalNeeded(FlexibleTaskDTO task) {
        int cumulative = safe(task.getCumulativeAllocatedTime());
        TaskNature nature = task.getTaskNature();

        // 1) FIXED_ESTIMATE or unspecified nature:
        if (nature == null || nature == TaskNature.FIXED_ESTIMATE) {
            int estimate = safe(task.getEstimatedDuration());
            return Math.max(estimate - cumulative, 0);
        }

        // 2) OPEN_ENDED (or any non-FIXED_ESTIMATE):
        //    -> schedule a fixed per-run amount; no global cap.
        int perRunTarget = safe(task.getTargetAllocatedTime());

        if (perRunTarget <= 0) {
            // no explicit target: choose something based on min/max block size
            int minBlock = resolveMinBlock(task);
            int maxBlock = resolveMaxBlock(task);
            // e.g. try to allocate 2 blocks worth per run, clamped
            perRunTarget = Math.min(maxBlock * 2, maxBlock * 4); // tweak if you like
            perRunTarget = Math.max(perRunTarget, minBlock);
        }

        return perRunTarget;
    }

    private int safe(Integer v) {
        return v != null ? v : 0;
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

