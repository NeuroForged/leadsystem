package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.Lead;

public interface LeadNotificationService {

    void notifyNewLead(Lead lead);
}
