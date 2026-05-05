package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.KbFetchJobStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KbFetchStatusStore {

    private final Map<String, KbFetchJobStatus> store = new ConcurrentHashMap<>();

    public void put(String jobId, KbFetchJobStatus status) {
        store.put(jobId, status);
    }

    public KbFetchJobStatus get(String jobId) {
        return store.get(jobId);
    }
}
