package com.scheduler.scheduling.controller;

import com.scheduler.scheduling.models.Schedule;
import com.scheduler.scheduling.services.TaskSchedulerService;
import com.scheduler.commoncode.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scheduling")
public class SchedulingController {

    private final TaskSchedulerService taskSchedulerService;
    private final JwtUtil jwtUtil;

    public SchedulingController(TaskSchedulerService taskSchedulerService, JwtUtil jwtUtil) {
        this.taskSchedulerService = taskSchedulerService;
        this.jwtUtil     = jwtUtil;
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ","");
        return jwtUtil.extractCustomerId(token);
    }

    /**
     * Returns a full schedule for the given customer,
     * pulling tasks and zones via gRPC under the hood.
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Schedule> getScheduleForCustomer(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (!customerId.equals(extractCustomerId(authHeader))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Schedule schedule = taskSchedulerService.scheduleTasksForCustomer(customerId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping
    public ResponseEntity<Schedule> getMySchedule(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        return ResponseEntity.ok(taskSchedulerService.scheduleTasksForCustomer(cid));
    }
}
