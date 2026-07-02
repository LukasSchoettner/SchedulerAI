package com.scheduler.scheduling.evaluator;

import com.scheduler.commoncode.dto.TaskDTO;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeWindowEvaluator implements ZoneEvaluator {

    private final LocalTime zoneStart;
    private final LocalTime zoneEnd;

    public TimeWindowEvaluator(LocalTime zoneStart, LocalTime zoneEnd) {
        this.zoneStart = zoneStart;
        this.zoneEnd = zoneEnd;
    }

    @Override
    public boolean isSatisfiedBy(TaskDTO task, LocalDateTime slotStart, LocalDateTime slotEnd) {
        LocalTime start = slotStart.toLocalTime();
        LocalTime end   = slotEnd.toLocalTime();

        // Check if the candidate slot is fully within [zoneStart, zoneEnd].
        // For instance:
        boolean startsAfterWindowOpen  = !start.isBefore(zoneStart);
        boolean endsBeforeWindowClose  = !end.isAfter(zoneEnd);

        return startsAfterWindowOpen && endsBeforeWindowClose;
    }
}
