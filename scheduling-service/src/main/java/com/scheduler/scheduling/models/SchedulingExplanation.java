package com.scheduler.scheduling.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingExplanation {
    private Long taskId;
    private String title;
    private String explanation;
    private List<String> reasons = new ArrayList<>();
}
