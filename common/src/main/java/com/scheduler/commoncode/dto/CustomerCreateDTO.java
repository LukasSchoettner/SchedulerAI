package com.scheduler.commoncode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerCreateDTO {

    @NotBlank(message = "Customername cannot be blank")
    private String customername;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    private String password;
}
