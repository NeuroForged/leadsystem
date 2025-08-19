package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CalendlyOAuthRequest {
    private String code;
    private String state;
}
