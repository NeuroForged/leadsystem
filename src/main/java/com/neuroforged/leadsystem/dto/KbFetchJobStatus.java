package com.neuroforged.leadsystem.dto;

public record KbFetchJobStatus(String jobId, String status, Integer docsCount, String error) {

    public static KbFetchJobStatus pending(String jobId) {
        return new KbFetchJobStatus(jobId, "PENDING", null, null);
    }

    public static KbFetchJobStatus running(String jobId) {
        return new KbFetchJobStatus(jobId, "RUNNING", null, null);
    }

    public static KbFetchJobStatus done(String jobId, int docsCount) {
        return new KbFetchJobStatus(jobId, "DONE", docsCount, null);
    }

    public static KbFetchJobStatus error(String jobId, String error) {
        return new KbFetchJobStatus(jobId, "ERROR", null, error);
    }
}
