package com.scheduler.commoncode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Data Transfer Object for a ZoneConfiguration, including its definitions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneConfigurationDTO {
    private Long id;
    private String name;
    private boolean active;

    // new fields
    private LocalTime startTime;
    private LocalTime endTime;

    private Long customerId;
    private List<ZoneDefinitionDTO> zones;
}

