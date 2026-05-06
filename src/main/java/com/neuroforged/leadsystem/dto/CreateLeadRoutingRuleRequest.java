package com.neuroforged.leadsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLeadRoutingRuleRequest {

    @NotBlank
    private String matchField;

    @NotBlank
    private String matchValue;

    @NotBlank
    private String assignTo;

    private Integer priority = 100;
}
