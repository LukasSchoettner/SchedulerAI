package com.scheduler.taskmanagement.controllers;

import com.scheduler.commoncode.dto.TaskDTO;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.taskmanagement.dto.TaskTemplateInstantiateRequest;
import com.scheduler.taskmanagement.dto.TaskTemplateRequest;
import com.scheduler.taskmanagement.dto.TaskTemplateResponse;
import com.scheduler.taskmanagement.services.TaskTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks/templates")
public class TaskTemplateController {

    private final TaskTemplateService templateService;
    private final JwtUtil jwtUtil;

    public TaskTemplateController(TaskTemplateService templateService, JwtUtil jwtUtil) {
        this.templateService = templateService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<List<TaskTemplateResponse>> list(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(templateService.list(extractCustomerId(authHeader)));
    }

    @PostMapping
    public ResponseEntity<TaskTemplateResponse> create(
            @RequestBody TaskTemplateRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.status(201).body(templateService.create(request, extractCustomerId(authHeader)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskTemplateResponse> update(
            @PathVariable Long id,
            @RequestBody TaskTemplateRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(templateService.update(id, request, extractCustomerId(authHeader)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        templateService.archive(id, extractCustomerId(authHeader));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/instantiate")
    public ResponseEntity<TaskDTO> instantiate(
            @PathVariable Long id,
            @RequestBody(required = false) TaskTemplateInstantiateRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.status(201).body(templateService.instantiate(id, request, extractCustomerId(authHeader)));
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractCustomerId(token);
    }
}
