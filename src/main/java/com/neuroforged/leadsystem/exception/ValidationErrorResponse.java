package com.neuroforged.leadsystem.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> fieldErrors;
    private String path;

    public ValidationErrorResponse(LocalDateTime timestamp, int status, String error, String message,
                                   String path, Map<String, String> fieldErrors) {
        super(timestamp, status, error, message, path);
        this.path = path;
        this.fieldErrors = fieldErrors;

    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
