package com.scheduler.scheduling.dto;

import com.scheduler.scheduling.models.DayPlanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DayPlanResponse(
        Long id,
        Long customerId,
        LocalDate planDate,
        DayPlanStatus status,
        LocalDateTime generatedAt,
        LocalDateTime confirmedAt,
        LocalDateTime reviewedAt,
        String planSignature,
        Integer freeGapMinutes,
        String tightSpotSummary,
        Boolean changedFromConfirmed,
        List<DayPlanItemResponse> items
) {
}
