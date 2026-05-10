package com.neuroforged.leadsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * LSB-152: Body for POST /api/contact from the marketing website.
 */
@Data
public class ContactRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email required")
    @Size(max = 255)
    private String email;

    @Size(max = 120)
    private String company;

    @NotBlank(message = "Subject is required")
    @Size(max = 255)
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 5000)
    private String message;

    @Size(max = 60)
    private String source;
}
