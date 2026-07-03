package com.scheduler.commoncode.dto;

import com.scheduler.commoncode.enums.MembershipLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Unified Customer DTO for create, update, and responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    // For updates and responses, id may be set; for creates, it should be null
    private Long id;

    @NotBlank(message = "Customer name cannot be blank")
    private String customername;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    // For create, password is required; for responses, it will be null
    private String password;

    // Active flag; optional for create (defaults to true), used in updates and responses
    private boolean active;

    private ZoneConfigurationDTO zoneConfiguration;

    private MembershipLevel membershipLevel;

    private SchedulingPreferenceDTO schedulingPreference;

    public CustomerDTO(Long id,
                       String customername,
                       String email,
                       String password,
                       boolean active,
                       ZoneConfigurationDTO zoneConfiguration,
                       MembershipLevel membershipLevel) {
        this.id = id;
        this.customername = customername;
        this.email = email;
        this.password = password;
        this.active = active;
        this.zoneConfiguration = zoneConfiguration;
        this.membershipLevel = membershipLevel;
    }
}
