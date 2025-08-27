package com.neuroforged.leadsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteeDTO {
    private String email;
    private String name;
}
