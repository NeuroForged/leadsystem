package com.neuroforged.leadsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientDto {
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Primary email is required")
    @Email(message = "Primary email must be a valid email address")
    private String primaryEmail;

    private String notificationEmails;

    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    private String websiteUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
