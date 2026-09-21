package com.scheduler.taskmanagement.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.taskmanagement.services.TaskService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskControllerUpdatePresenceTest {

    private final TaskService taskService = mock(TaskService.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final TaskController controller = new TaskController(taskService, jwtUtil, objectMapper);

    @Test
    void explicitNullDeadlineAndZeroPriorityArePassedAsPresentFields() throws Exception {
        when(jwtUtil.extractCustomerId("token")).thenReturn(7L);
        FlexibleTaskDTO updated = new FlexibleTaskDTO();
        when(taskService.updateTaskFromRest(eq(11L), any(), eq(7L), any()))
                .thenReturn(Optional.of(updated));
        var body = objectMapper.readTree("""
                {
                  "type": "FLEXIBLE",
                  "dueDate": null,
                  "priority": 0
                }
                """);

        controller.updateTask(11L, body, "Bearer token");

        verify(taskService).updateTaskFromRest(
                eq(11L),
                any(FlexibleTaskDTO.class),
                eq(7L),
                eq(Set.of("type", "dueDate", "priority"))
        );
    }

    @Test
    void omittedDeadlineAndPriorityStayAbsentFromPresenceSet() throws Exception {
        when(jwtUtil.extractCustomerId("token")).thenReturn(7L);
        FlexibleTaskDTO updated = new FlexibleTaskDTO();
        when(taskService.updateTaskFromRest(eq(11L), any(), eq(7L), any()))
                .thenReturn(Optional.of(updated));
        var body = objectMapper.readTree("""
                {
                  "type": "FLEXIBLE",
                  "title": "Rename only"
                }
                """);

        controller.updateTask(11L, body, "Bearer token");

        verify(taskService).updateTaskFromRest(
                eq(11L),
                any(FlexibleTaskDTO.class),
                eq(7L),
                eq(Set.of("type", "title"))
        );
    }
}
