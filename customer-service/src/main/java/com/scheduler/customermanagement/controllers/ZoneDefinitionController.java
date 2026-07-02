// src/main/java/com/scheduler/customermanagement/controllers/ZoneDefinitionController.java
package com.scheduler.customermanagement.controllers;

import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.customermanagement.services.ZoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/customers/zones/{configId}/definitions")
public class ZoneDefinitionController {

    private final ZoneService zoneService;
    private final JwtUtil jwtUtil;

    public ZoneDefinitionController(ZoneService zoneService, JwtUtil jwtUtil) {
        this.zoneService = zoneService;
        this.jwtUtil     = jwtUtil;
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ","");
        return jwtUtil.extractCustomerId(token);
    }

    @GetMapping
    public ResponseEntity<List<ZoneDefinitionDTO>> listDefinitions(
            @PathVariable Long configId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        return ResponseEntity.ok(zoneService.listDefinitions(cid, configId));
    }

    @PostMapping
    public ResponseEntity<ZoneDefinitionDTO> createDefinition(
            @PathVariable Long configId,
            @Valid @RequestBody ZoneDefinitionDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        ZoneDefinitionDTO created = zoneService.addDefinition(cid, configId, dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{defId}")
    public ResponseEntity<ZoneDefinitionDTO> updateDefinition(
            @PathVariable Long configId,
            @PathVariable Long defId,
            @Valid @RequestBody ZoneDefinitionDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        ZoneDefinitionDTO updated = zoneService.updateDefinition(cid, configId, defId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{defId}")
    public ResponseEntity<Void> deleteDefinition(
            @PathVariable Long configId,
            @PathVariable Long defId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long cid = extractCustomerId(authHeader);
        boolean removed = zoneService.deleteDefinition(cid, configId, defId);
        return removed
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
