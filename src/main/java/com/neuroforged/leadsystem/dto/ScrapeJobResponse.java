package com.neuroforged.leadsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScrapeJobResponse {
    @JsonProperty("job_id")
    private String jobId;
    private String status;
    private boolean reused;
}
