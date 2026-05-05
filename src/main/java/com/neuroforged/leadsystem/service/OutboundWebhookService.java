package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;

public interface OutboundWebhookService {
    void notifyWebhook(Lead lead, Client client);
}
