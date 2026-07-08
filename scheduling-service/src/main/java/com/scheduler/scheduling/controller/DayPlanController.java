package com.scheduler.scheduling.controller;

import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.scheduling.dto.DayPlanResponse;
import com.scheduler.scheduling.services.DayPlanService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/day-plans")
public class DayPlanController {

    private final DayPlanService dayPlanService;
    private final JwtUtil jwtUtil;

    public DayPlanController(DayPlanService dayPlanService, JwtUtil jwtUtil) {
        this.dayPlanService = dayPlanService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/generate")
    public ResponseEntity<DayPlanResponse> generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAfter,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.generatePlan(
                extractCustomerId(authHeader),
                date != null ? date : LocalDate.now(),
                startAfter
        ));
    }

    @GetMapping("/me/{date}")
    public ResponseEntity<DayPlanResponse> getMine(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.getPlan(extractCustomerId(authHeader), date));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<DayPlanResponse> confirm(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.confirm(extractCustomerId(authHeader), id));
    }

    @PostMapping("/{id}/items/{itemId}/skip-today")
    public ResponseEntity<DayPlanResponse> skipToday(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.skipToday(extractCustomerId(authHeader), id, itemId));
    }

    @PostMapping("/{id}/items/{itemId}/keep-free")
    public ResponseEntity<DayPlanResponse> keepFree(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.keepFree(extractCustomerId(authHeader), id, itemId));
    }

    @PostMapping("/{id}/items/{itemId}/complete")
    public ResponseEntity<DayPlanResponse> complete(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.complete(extractCustomerId(authHeader), id, itemId));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<DayPlanResponse> regenerate(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAfter,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.regenerate(extractCustomerId(authHeader), id, startAfter));
    }

    @PostMapping("/{id}/items/{itemId}/reschedule")
    public ResponseEntity<DayPlanResponse> reschedule(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody RescheduleRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(dayPlanService.rescheduleFlexibleItem(
                extractCustomerId(authHeader),
                id,
                itemId,
                request != null ? request.startAfter() : null,
                request != null ? request.reason() : null,
                request != null ? request.remainingMinutes() : null
        ));
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractCustomerId(token);
    }

    public record RescheduleRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAfter,
            String reason,
            Integer remainingMinutes
    ) {
    }
}
