package com.scheduler.scheduling.models;

import com.scheduler.commoncode.dto.TaskDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ScheduledTask {
    private TaskDTO task;
    // For flexible tasks that might be splitted, we can have multiple assigned slots
    private List<TimeSlot> assignedSlots = new ArrayList<>();
    private SchedulingExplanation explanation;

    public ScheduledTask(TaskDTO task, TimeSlot singleSlot) {
        this.task = task;
        if (singleSlot != null) {
            this.assignedSlots.add(singleSlot);
        }
    }

    public ScheduledTask(TaskDTO task, List<TimeSlot> multiSlots) {
        this.task = task;
        if (multiSlots != null) {
            this.assignedSlots.addAll(multiSlots);
        }
    }
}
