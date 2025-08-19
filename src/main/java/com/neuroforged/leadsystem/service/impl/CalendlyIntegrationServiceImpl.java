package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.repository.CalendlyIntegrationRepository;
import com.neuroforged.leadsystem.service.CalendlyIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendlyIntegrationServiceImpl implements CalendlyIntegrationService {

    private final CalendlyIntegrationRepository integrationRepository;

    @Override
    public boolean markIntegrationComplete(String state) {
        log.info("CalendlyIntegrationService.markIntegrationComplete");
        return integrationRepository.findByState(state).map(integration -> {
            integration.setCompleted(true);
            integrationRepository.save(integration);
            log.info("integration: client: {}, id: {}, state: {}", integration.getClient(), integration.getId(), integration.getState());
            return true;
        }).orElse(false);
    }
}
