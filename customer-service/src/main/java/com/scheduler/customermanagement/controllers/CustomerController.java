// src/main/java/com/scheduler/customermanagement/controllers/CustomerController.java
package com.scheduler.customermanagement.controllers;

import com.scheduler.commoncode.dto.CustomerDTO;
import com.scheduler.commoncode.dto.ZoneConfigurationDTO;
import com.scheduler.commoncode.dto.ZoneDefinitionDTO;
import com.scheduler.commoncode.security.JwtUtil;
import com.scheduler.customermanagement.services.CustomerService;
import com.scheduler.customermanagement.services.ZoneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final ZoneService zoneService;
    private final JwtUtil jwtUtil;

    public CustomerController(
            CustomerService customerService,
            ZoneService zoneService,
            JwtUtil jwtUtil
    ) {
        this.customerService = customerService;
        this.zoneService     = zoneService;
        this.jwtUtil         = jwtUtil;
    }

    private Long extractCustomerId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractCustomerId(token);
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CustomerDTO dto) {
        CustomerDTO created = customerService.createCustomer(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAll() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerDTO> getMe(@RequestHeader("Authorization") String authHeader) {
        Long customerId = extractCustomerId(authHeader);
        return customerService.getCustomerById(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDTO dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (!id.equals(extractCustomerId(authHeader))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return customerService.updateCustomer(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (!id.equals(extractCustomerId(authHeader))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return customerService.deleteCustomer(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // --- Zone Configuration Endpoints ---

    @GetMapping("/zones")
    public ResponseEntity<List<ZoneConfigurationDTO>> getAllZoneConfigs(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        List<ZoneConfigurationDTO> dtos = zoneService.listConfigs(customerId);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/zones")
    public ResponseEntity<ZoneConfigurationDTO> createZoneConfig(
            @Valid @RequestBody ZoneConfigurationDTO configDto,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        ZoneConfigurationDTO result = zoneService.createZoneConfig(
                customerId,
                configDto.getName(),
                configDto.isActive(),
                configDto.getStartTime(),
                configDto.getEndTime()
        );
        return ResponseEntity.status(201).body(result);
    }

    @PutMapping("/zones/{configId}/activate")
    public ResponseEntity<ZoneConfigurationDTO> activateZoneConfig(
            @PathVariable Long configId,
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        ZoneConfigurationDTO dto = zoneService.activateConfig(customerId, configId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/zones/active")
    public ResponseEntity<ZoneConfigurationDTO> getActiveZoneConfig(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long customerId = extractCustomerId(authHeader);
        ZoneConfigurationDTO dto = zoneService.getActiveConfig(customerId);
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/zones/{configId}")
    public ResponseEntity<Void> zoneConfigDelete(@PathVariable Long configId,
                                                 @RequestHeader("Authorization") String authHeader) {
        Long customerId = extractCustomerId(authHeader);

        return zoneService.deleteZoneConfig(customerId, configId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

}
