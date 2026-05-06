package com.neuroforged.leadsystem.dto;

import com.neuroforged.leadsystem.entity.NotificationChannelType;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class CreateNotificationChannelRequest {

    @NotNull
    private NotificationChannelType channelType;

    @NotBlank
    private String destination;

    @NotEmpty
    private Set<NotificationEventType> events;
}
