package com.neuroforged.leadsystem.dto;

import com.neuroforged.leadsystem.entity.NotificationChannelType;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class NotificationChannelDto {
    private Long id;
    private Long clientId;
    private NotificationChannelType channelType;
    private String destination;
    private Set<NotificationEventType> events;
    private LocalDateTime createdAt;
}
