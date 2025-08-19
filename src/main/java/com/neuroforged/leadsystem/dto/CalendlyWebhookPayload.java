package com.neuroforged.leadsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CalendlyWebhookPayload {
    private String event;
    private Payload payload;

    @Data
    public static class Payload {
        @JsonProperty("event_type")
        private EventType eventType;

        @JsonProperty("scheduled_event")
        private ScheduledEvent scheduledEvent;

        private Invitee invitee;
        private String event;

        @Data
        public static class EventType {
            private String uuid;
            private String name;
            private String slug;
        }

        @Data
        public static class ScheduledEvent {
            @JsonProperty("start_time")
            private String startTime;

            @JsonProperty("end_time")
            private String endTime;

            private Location location;

            @Data
            public static class Location {
                private String type;

                @JsonProperty("join_url")
                private String joinUrl;
            }
        }

        @Data
        public static class Invitee {
            private String uuid;
            private String email;
            private String name;

            @JsonProperty("created_at")
            private String createdAt;

            private boolean canceled;
        }
    }
}

