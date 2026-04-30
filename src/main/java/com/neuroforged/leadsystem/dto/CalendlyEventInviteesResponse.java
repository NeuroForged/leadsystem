package com.neuroforged.leadsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendlyEventInviteesResponse {

    private List<Invitee> collection;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Invitee {
        private String uri;
        private String email;
        private String name;
        private String status;
    }
}
