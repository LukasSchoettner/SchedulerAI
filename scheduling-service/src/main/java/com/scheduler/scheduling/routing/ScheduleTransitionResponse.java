package com.scheduler.scheduling.routing;

public record ScheduleTransitionResponse(
        Long fromDayPlanItemId,
        Long toDayPlanItemId,
        String fromTitle,
        String toTitle,
        Long fromAddressId,
        Long toAddressId,
        String fromAddressText,
        String toAddressText,
        Integer availableMinutes,
        Integer estimatedTravelMinutes,
        Boolean feasible,
        TravelWarningCode warningCode,
        String warningMessage
) {
}
