package com.scheduler.customermanagement.controllers;

import com.scheduler.commoncode.dto.SchedulingPreferenceDTO;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.customermanagement.services.SchedulingPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customers/preferences")
public class SchedulingPreferenceController {
    private final SchedulingPreferenceService service;
    private final JwtUtil jwtUtil;

    public SchedulingPreferenceController(SchedulingPreferenceService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<SchedulingPreferenceDTO> getPreferences(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        return service.getPreferences(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<SchedulingPreferenceDTO> savePreferences(
            @RequestBody SchedulingPreferenceDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        return ResponseEntity.ok(service.savePreferences(customerId, dto));
    }

    @GetMapping("/onboarding-status")
    public ResponseEntity<Map<String, Boolean>> onboardingStatus(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        return ResponseEntity.ok(Map.of("completed", service.hasActivePreferences(customerId)));
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractCustomerId(token);
    }
}
