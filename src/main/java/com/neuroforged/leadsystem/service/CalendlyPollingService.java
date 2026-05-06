package com.neuroforged.leadsystem.service;

public interface CalendlyPollingService {
    /** Polls all accounts and returns the number of meetings synced. */
    int pollAllAccounts();
}
