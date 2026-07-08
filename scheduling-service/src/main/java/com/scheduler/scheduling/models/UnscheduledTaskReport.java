package com.scheduler.scheduling.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnscheduledTaskReport {
    private Long taskId;
    private String title;
    private String category;
    private UnscheduledReasonCode reasonCode;
    private String explanation;
}
