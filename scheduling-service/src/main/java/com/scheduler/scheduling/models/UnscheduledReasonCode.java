package com.scheduler.scheduling.models;

public enum UnscheduledReasonCode {
    NO_AVAILABLE_SLOT,
    DURATION_TOO_LONG,
    OUTSIDE_ALLOWED_WINDOW,
    AFTER_LATEST_END,
    BEFORE_EARLIEST_START,
    CONFLICTS_WITH_FIXED_TASK,
    UNKNOWN
}
