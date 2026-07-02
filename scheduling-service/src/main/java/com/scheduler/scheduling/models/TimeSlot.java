package com.scheduler.scheduling.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
public class TimeSlot {
    private LocalDateTime start;
    private LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("TimeSlot start and end must both be non-null");
        }
        this.start = start;
        this.end   = end;
    }


    public long durationMinutes() {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public boolean overlaps(TimeSlot other) {
        return !(other.getEnd().isBefore(this.start) || other.getStart().isAfter(this.end));
    }

    /**
     * Split the current slot by the 'occupied' timeslot,
     * returning up to two smaller free intervals
     */
    public List<TimeSlot> splitBy(TimeSlot occupied) {
        List<TimeSlot> result = new ArrayList<>();

        // If there's space before
        if (occupied.getStart().isAfter(this.start)) {
            TimeSlot left = new TimeSlot(this.start, occupied.getStart());
            if (left.durationMinutes() > 0) {
                result.add(left);
            }
        }
        // If there's space after
        if (occupied.getEnd().isBefore(this.end)) {
            TimeSlot right = new TimeSlot(occupied.getEnd(), this.end);
            if (right.durationMinutes() > 0) {
                result.add(right);
            }
        }
        return result;
    }
}


