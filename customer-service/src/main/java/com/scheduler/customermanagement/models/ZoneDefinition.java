package com.scheduler.customermanagement.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "zone_definitions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;  // e.g. "Sport zone"

    // Example dayMask approach (bitmask). 127 means Sunday to Saturday
    private int dayMask;

    private LocalTime startTime; // or LocalTime
    private LocalTime endTime;   // or LocalTime

    @ElementCollection
    @CollectionTable(name = "zone_allowed_categories", joinColumns = @JoinColumn(name = "zone_definition_id"))
    @Column(name = "category")
    private Set<String> allowedCategories = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "zone_excluded_categories", joinColumns = @JoinColumn(name = "zone_definition_id"))
    @Column(name = "category")
    private Set<String> excludedCategories = new HashSet<>();

    private Integer priorityOverrideThreshold;

    private String primaryCategory;

    @ElementCollection
    @CollectionTable(name = "zone_secondary_categories", joinColumns = @JoinColumn(name = "zone_definition_id"))
    @Column(name = "category")
    private Set<String> secondaryCategories = new HashSet<>();

    private String behaviorMode = "STRICT";

    private String targetPlacementMode = "ALLOW_ELSEWHERE";

    @Column(name = "zone_config_id", nullable = false)
    private Long zoneConfigId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZoneDefinition)) return false;
        ZoneDefinition other = (ZoneDefinition) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
