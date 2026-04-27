package com.neuroforged.leadsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalendlyScheduledEventsResponse {

    private List<Event> collection;
    private Pagination pagination;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String uri;
        private String name;
        private String status;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("event_type")
        private String eventType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private int count;

        @JsonProperty("next_page_token")
        private String nextPageToken;
    }
}
