package com.scheduler.routing.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class DistanceCache {
    @Id
    @GeneratedValue
    private Long id;
    private Double originLat, originLon;
    private Double destLat, destLon;
    private Double travelTimeMinutes;
    private LocalDateTime lastUpdated;
}
