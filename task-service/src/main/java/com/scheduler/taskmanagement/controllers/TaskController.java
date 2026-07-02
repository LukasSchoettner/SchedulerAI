// src/main/java/com/scheduler/taskmanagement/controllers/TaskController.java
package com.scheduler.taskmanagement.controllers;

import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.dto.UpdateTaskStatusDTO;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.taskmanagement.services.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final JwtUtil jwtUtil;

    public TaskController(TaskService taskService, JwtUtil jwtUtil) {
        this.taskService = taskService;
        this.jwtUtil     = jwtUtil;
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ","");
        return jwtUtil.extractCustomerId(token);
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @RequestBody TaskDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        TaskDTO saved = taskService.createTask(dto, cid);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        var list = taskService.listTasksForCustomer(cid);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        return taskService.getTaskById(id, cid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        return taskService.updateTask(id, dto, cid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        boolean ok = taskService.deleteTask(id, cid);
        return ok
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateTaskStatusDTO req,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        var updated = taskService.updateStatus(id, cid, com.scheduler.commoncode.enums.TaskStatus.valueOf(req.status()));
        return ResponseEntity.ok(updated);
    }

    // Convenience if you want a simple “complete” route:
    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskDTO> complete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        var updated = taskService.updateStatus(id, cid, com.scheduler.commoncode.enums.TaskStatus.COMPLETED);
        return ResponseEntity.ok(updated);
    }


    @PostMapping("/generate-dummy")
    public ResponseEntity<Void> generateDummy(
            @RequestParam(defaultValue="10") int count,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        taskService.generateDummyTasks(count, cid);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/needs-completion-check")
    public ResponseEntity<List<TaskDTO>> getTasksNeedingCompletionCheck(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        var allTasks = taskService.listTasksForCustomer(cid);
        LocalDateTime now = LocalDateTime.now();

        List<TaskDTO> needing = allTasks.stream()
                // only tasks not completed
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                // apply your rule: dueDate passed OR estimated time used up (for flexible)
                .filter(t -> needsCompletionPrompt(t, now))
                .collect(Collectors.toList());

        return ResponseEntity.ok(needing);
    }

    // put this inside TaskController

    // configurable grace so you don't prompt on tasks that *just* became due
//    private static final int DUE_GRACE_MINUTES = 5;
//
//    private boolean needsCompletionPrompt(TaskDTO t, LocalDateTime now) {
//        // Safety: null status handled by caller, but keep it robust
//        if (t == null) return false;
//
//        // If explicitly cancelled/archived, you probably don't want prompts
//        // (adjust to your enum)
//        if (t.getStatus() == TaskStatus.CANCELLED) return false;
//
//        // 1) Reminder passed -> prompt
//        if (t.getReminderDate() != null && !t.getReminderDate().isAfter(now)) {
//            return true;
//        }
//
//        // 2) Due date passed (with grace) -> prompt
//        if (t.getDueDate() != null && t.getDueDate().plusMinutes(DUE_GRACE_MINUTES).isBefore(now)) {
//            return true;
//        }
//
//        // 3) Type-specific rules (only if you actually have subtypes)
//        // If your TaskDTO is abstract with Jackson subtypes (FixedTaskDTO/FlexibleTaskDTO),
//        // this works nicely.
//
//        // FixedTaskDTO: if it has an end time and it passed -> prompt
//        if (t instanceof com.scheduler.commoncode.dto.FixedTaskDTO ft) {
//            // Adjust field names to your actual DTO:
//            // Example possibilities: getStartTime(), getEndTime(), getDurationMinutes()
//            LocalDateTime end = null;
//
//            try {
//                // if you have explicit endDate:
//                // end = ft.getEndDate();
//
//                // or if you have startDate + duration:
//                if (ft.getStartDateTime() != null && ft.getDurationMinutes() != null) {
//                    end = ft.getStartDate().plusMinutes(ft.getDurationMinutes());
//                }
//            } catch (Exception ignored) {
//                // if fields don't exist / differ, ignore and fall back to due/reminder
//            }
//
//            if (end != null && end.plusMinutes(DUE_GRACE_MINUTES).isBefore(now)) {
//                return true;
//            }
//        }
//
//        // FlexibleTaskDTO: if it has an "estimated minutes" and a "scheduled end" or "planned start"
//        if (t instanceof com.scheduler.commoncode.dto.FlexibleTaskDTO fl) {
//            try {
//                // Common patterns:
//                // - fl.getScheduledStart(), fl.getScheduledEnd()
//                // - fl.getDurationMinutes() or fl.getEstimatedDurationMinutes()
//                // - fl.getPlannedStart() etc.
//                LocalDateTime scheduledEnd = null;
//
//                // if you have explicit scheduled end:
//                // scheduledEnd = fl.getScheduledEnd();
//
//                // or if you have scheduled start + estimated duration:
//                if (fl.getScheduledStart() != null && fl.getEstimatedDurationMinutes() != null) {
//                    scheduledEnd = fl.getScheduledStart().plusMinutes(fl.getEstimatedDurationMinutes());
//                }
//
//                if (scheduledEnd != null && scheduledEnd.plusMinutes(DUE_GRACE_MINUTES).isBefore(now)) {
//                    return true;
//                }
//            } catch (Exception ignored) {
//                // ignore, fall back to due/reminder
//            }
//        }
//
//        return false;
//    }
    private static final int DUE_GRACE_MINUTES = 5;

    private boolean needsCompletionPrompt(TaskDTO t, LocalDateTime now) {
        if (t == null) return false;
        if (t.getStatus() == TaskStatus.CANCELLED) return false;

        if (t.getReminderDate() != null && !t.getReminderDate().isAfter(now)) return true;

        return t.getDueDate() != null
                && t.getDueDate().plusMinutes(DUE_GRACE_MINUTES).isBefore(now);
    }


}
