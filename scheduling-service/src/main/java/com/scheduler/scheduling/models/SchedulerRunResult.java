package com.scheduler.scheduling.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerRunResult {
    private List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private List<UnscheduledTaskReport> unscheduledTasks = new ArrayList<>();
    private List<SchedulingExplanation> explanations = new ArrayList<>();
}
