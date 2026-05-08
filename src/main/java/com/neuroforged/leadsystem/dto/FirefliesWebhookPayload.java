package com.neuroforged.leadsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Payload received from Fireflies.ai meeting-transcript webhooks.
 * Fields are mapped from Fireflies API v1 schema — unknown fields ignored.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FirefliesWebhookPayload {

    /** Event type, e.g. "Transcription completed" */
    private String type;

    /** Top-level meeting ID (mirrors meeting.id) */
    @JsonProperty("meetingId")
    private String meetingId;

    private Meeting meeting;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meeting {
        private String id;
        private String title;

        /** Meeting start time as millisecond epoch (UTC) */
        @JsonProperty("start_time")
        private Long startTime;

        /** Meeting end time as millisecond epoch (UTC) */
        @JsonProperty("end_time")
        private Long endTime;

        private List<Participant> participants;

        private Summary summary;

        /** Full transcript as a single text block (may be absent if not enabled). */
        private String transcript;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Participant {
        private String email;
        private String displayName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {
        private String overview;
        @JsonProperty("action_items")
        private String actionItems;
        private String keywords;
    }
}
