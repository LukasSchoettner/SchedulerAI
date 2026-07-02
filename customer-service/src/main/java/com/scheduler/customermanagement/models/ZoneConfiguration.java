package com.scheduler.customermanagement.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "zone_configurations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;        // e.g. "Regular", "Holiday"
    private boolean active;     // if this config is currently active
    private LocalTime startTime;
    private LocalTime endTime;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_config_id")  // <- FK lives in ZoneDefinition
    private Set<ZoneDefinition> zones = new HashSet<>();

    public void addZoneDefinition(ZoneDefinition zoneDefinition) {
        zones.add(zoneDefinition);
        zoneDefinition.setZoneConfigId(this.id);
    }

    public void removeZoneDefinition(ZoneDefinition zoneDefinition) {
        zones.remove(zoneDefinition);
        zoneDefinition.setZoneConfigId(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZoneConfiguration)) return false;
        ZoneConfiguration other = (ZoneConfiguration) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
