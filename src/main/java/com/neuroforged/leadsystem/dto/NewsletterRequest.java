package com.neuroforged.leadsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * LSB-152: Body for POST /api/newsletter from the marketing website.
 */
@Data
public class NewsletterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email required")
    @Size(max = 255)
    private String email;

    @Size(max = 60)
    private String source;
}
