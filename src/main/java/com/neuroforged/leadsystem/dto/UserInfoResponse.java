package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoResponse {
    private String email;
    private String role;
    private Long clientId;
}
