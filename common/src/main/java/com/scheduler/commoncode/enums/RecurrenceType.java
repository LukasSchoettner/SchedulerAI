package com.scheduler.commoncode.enums;

public enum RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    INTERVAL,          // every X days
    TIMES_PER_PERIOD   // e.g. X times per week/month/year
}

