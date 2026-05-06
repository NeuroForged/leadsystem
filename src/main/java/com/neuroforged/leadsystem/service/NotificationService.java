package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.NotificationEventType;

import java.util.Map;

public interface NotificationService {

    void notify(Long clientId, NotificationEventType event, Map<String, String> context);
}
