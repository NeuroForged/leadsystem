package com.neuroforged.leadsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientUserDto {
    private Long id;
    private String email;
    private String role;
    private Long clientId;
}
