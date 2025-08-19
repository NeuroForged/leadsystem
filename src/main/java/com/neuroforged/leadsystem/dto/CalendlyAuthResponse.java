package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CalendlyAuthResponse {
    private String authorizationUrl;
    private String state;
}
