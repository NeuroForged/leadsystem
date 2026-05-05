package com.neuroforged.leadsystem.dto;

import com.neuroforged.leadsystem.entity.MeetingOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MeetingOutcomeRequest {
    @NotNull
    private MeetingOutcome outcome;
}
