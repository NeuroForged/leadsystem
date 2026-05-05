package com.neuroforged.leadsystem.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ScraperStatusResponse {
    private String id;
    private String status;
    private ScraperResult result;
    private String error;

    @Data
    public static class ScraperResult {
        private int scraped;
        private int errors;
        private Map<String, String> files;
    }
}
