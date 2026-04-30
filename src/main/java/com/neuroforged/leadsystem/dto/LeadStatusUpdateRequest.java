package com.neuroforged.leadsystem.dto;

import com.neuroforged.leadsystem.entity.LeadStatus;
import lombok.Data;

@Data
public class LeadStatusUpdateRequest {
    private LeadStatus status;
}
