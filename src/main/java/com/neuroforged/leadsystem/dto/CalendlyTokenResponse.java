package com.neuroforged.leadsystem.dto;

import lombok.Data;

@Data
public class CalendlyTokenResponse {
        private String accessToken;
        private String refreshToken;
        private String organization;
        private String owner;
        private String ownerType;
}
