package com.neuroforged.leadsystem.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LeadRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Business name is required")
    @Size(max = 100, message = "Business name must be under 100 characters")
    private String businessName;

    @NotBlank(message = "Business type is required")
    @Size(max = 100, message = "Business type must be under 100 characters")
    private String businessType;

    @NotBlank(message = "Customer type is required")
    @Size(max = 100, message = "Customer type must be under 100 characters")
    private String customerType;

    @NotBlank(message = "Traffic source is required")
    @Size(max = 100, message = "Traffic source must be under 100 characters")
    private String trafficSource;

    @Min(value = 0, message = "Monthly leads must be at least 0")
    @Max(value = 10000, message = "Monthly leads seems unrealistically high")
    private int monthlyLeads;

    @DecimalMin(value = "0.00001", inclusive = true, message = "Conversion rate must be greater than 0")
    @DecimalMax(value = "100.0", message = "Conversion rate must be less than or equal to 100")
    private double conversionRate;

    @DecimalMin(value = "0.01", message = "Cost per lead must be greater than 0")
    private double costPerLead;

    @DecimalMin(value = "0.01", message = "Client value must be greater than 0")
    private double clientValue;

    @Min(value = 1)
    @Max(value = 100)
    private int leadScore;

    @NotBlank(message = "Lead challenge is required")
    @Size(max = 250, message = "Lead challenge must be under 250 characters")
    private String leadChallenge;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    /** Optional free-form map of everything the capturing channel collected. Bounded in
     *  sanitize(): at most 40 keys, keys ≤ 64 chars, values ≤ 500 chars. */
    private Map<String, String> capturedFields;

    public void sanitize() {
        if (email != null) email = email.trim().toLowerCase();
        if (businessName != null) businessName = businessName.trim();
        if (businessType != null) businessType = businessType.trim();
        if (customerType != null) customerType = customerType.trim();
        if (trafficSource != null) trafficSource = trafficSource.trim();
        if (leadChallenge != null) leadChallenge = leadChallenge.trim();
        if (clientId != null) clientId = clientId.trim();
        if (capturedFields != null) {
            Map<String, String> bounded = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : capturedFields.entrySet()) {
                if (bounded.size() >= 40) break;
                if (e.getKey() == null || e.getKey().isBlank()) continue;
                String k = e.getKey().trim();
                String v = e.getValue() == null ? "" : e.getValue();
                bounded.put(k.length() > 64 ? k.substring(0, 64) : k, v.length() > 500 ? v.substring(0, 500) : v);
            }
            capturedFields = bounded;
        }
    }
}
