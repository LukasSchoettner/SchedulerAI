package com.scheduler.taskmanagement.controllers;

import com.scheduler.commoncode.dto.FlexibleTaskDTO;
import com.scheduler.commoncode.enums.TaskNature;
import com.scheduler.commoncode.enums.TaskStatus;
import com.scheduler.commoncode.enums.TaskType;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.taskmanagement.dto.TaskTemplateResponse;
import com.scheduler.taskmanagement.services.TaskService;
import com.scheduler.taskmanagement.services.TaskTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskTemplateRouteTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskTemplateService templateService = mock(TaskTemplateService.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new TaskTemplateController(templateService, jwtUtil),
            new TaskController(taskService, jwtUtil)
    ).build();

    @Test
    void templatesRouteDoesNotHitTaskIdRoute() throws Exception {
        when(jwtUtil.extractCustomerId("token")).thenReturn(77L);
        when(templateService.list(77L)).thenReturn(List.of(TaskTemplateResponse.builder()
                .id(1L)
                .title("Laundry")
                .category("Duty")
                .defaultType(TaskType.FLEXIBLE)
                .usageCount(0)
                .build()));

        mvc.perform(get("/tasks/templates").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Laundry"));

        verify(templateService).list(77L);
        verify(taskService, never()).getTaskById(anyLong(), anyLong());
    }

    @Test
    void numericTaskIdRouteStillWorks() throws Exception {
        when(jwtUtil.extractCustomerId("token")).thenReturn(77L);
        when(taskService.getTaskById(123L, 77L)).thenReturn(Optional.of(new FlexibleTaskDTO(
                123L,
                "Existing task",
                TaskType.FLEXIBLE,
                3,
                LocalDateTime.of(2026, 7, 15, 23, 59),
                LocalDateTime.of(2026, 7, 15, 20, 0),
                TaskStatus.PENDING,
                "NONE",
                null,
                "Work",
                null,
                null,
                60,
                10,
                null,
                null,
                TaskNature.FIXED_ESTIMATE,
                30,
                120,
                false,
                false,
                0,
                0
        )));

        mvc.perform(get("/tasks/123").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Existing task"));

        verify(taskService).getTaskById(123L, 77L);
        verify(templateService, never()).list(anyLong());
    }
}
