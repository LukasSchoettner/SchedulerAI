package com.scheduler.customermanagement.models;

import com.scheduler.commoncode.enums.MembershipLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic customer data (customername, email, etc.)
    @NotBlank
    @Column(nullable = false)
    private String customerName;

    private String password;

    @NotBlank
    @Column(nullable = false)
    @Email
    private String email;

    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipLevel membershipLevel = MembershipLevel.BASIC;

    // If you're storing zone configs here:
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ZoneConfiguration> zoneConfigurations = new HashSet<>();

    public void addZoneConfiguration(ZoneConfiguration zoneConfig) {
        zoneConfigurations.add(zoneConfig);
        zoneConfig.setCustomerId(this.getId());
    }

    public void removeZoneConfiguration(ZoneConfiguration zoneConfig) {
        zoneConfigurations.remove(zoneConfig);
        zoneConfig.setCustomerId(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
