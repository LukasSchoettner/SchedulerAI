package com.scheduler.scheduling.evaluator;

import com.scheduler.commoncode.dto.TaskDTO;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class DayMaskEvaluator implements ZoneEvaluator {

    private final int dayMask; // e.g. 127 = all days

    public DayMaskEvaluator(int dayMask) {
        this.dayMask = dayMask;
    }

    @Override
    public boolean isSatisfiedBy(TaskDTO task, LocalDateTime slotStart, LocalDateTime slotEnd) {
        DayOfWeek dow = slotStart.getDayOfWeek();        // MONDAY=1…SUNDAY=7
        int requiredBit = 1 << (dow.getValue() - 1);     // 1<<(0) for Monday … 1<<(6) for Sunday
        return (dayMask & requiredBit) != 0;
    }

}
