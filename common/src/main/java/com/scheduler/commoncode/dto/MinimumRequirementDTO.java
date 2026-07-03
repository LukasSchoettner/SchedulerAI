package com.scheduler.commoncode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinimumRequirementDTO {
    private String category;
    private String type;
    private Integer amount;
    private String period;
}
