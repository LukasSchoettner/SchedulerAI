package com.scheduler.scheduling.models;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
public class Schedule {

    private List<ScheduledTask> scheduledTasks;

    public void setScheduledTasks(List<ScheduledTask> scheduledTasks) {
        if (scheduledTasks != null) {
            // sort by each task’s earliest assigned slot start
            scheduledTasks.sort(Comparator.comparing(st ->
                    st.getAssignedSlots().stream()
                            .map(TimeSlot::getStart)
                            .min(LocalDateTime::compareTo)
                            .orElse(LocalDateTime.MIN)
            ));
        }
        this.scheduledTasks = scheduledTasks;
    }

    /** If you really need addAll, have it delegate back to the setter: */
    public void addAll(List<ScheduledTask> tasks) {
        setScheduledTasks(tasks);
    }
}

