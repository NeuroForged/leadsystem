package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadRoutingRule;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.LeadRoutingRuleRepository;
import com.neuroforged.leadsystem.service.LeadRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadRoutingServiceImpl implements LeadRoutingService {

    private final LeadRoutingRuleRepository routingRuleRepository;
    private final LeadSystemMetrics metrics;

    @Override
    public void route(Lead lead) {
        Long clientLongId;
        try {
            clientLongId = Long.parseLong(lead.getClientId());
        } catch (NumberFormatException e) {
            log.warn("LeadRouting: cannot parse clientId '{}' — skipping", lead.getClientId());
            return;
        }

        List<LeadRoutingRule> rules = routingRuleRepository.findByClientIdOrderByPriorityAscIdAsc(clientLongId);
        if (rules.isEmpty()) return;

        Map<String, String> fieldValues = buildFieldMap(lead);

        for (LeadRoutingRule rule : rules) {
            String fieldValue = fieldValues.get(rule.getMatchField().toLowerCase());
            if (fieldValue != null && fieldValue.equalsIgnoreCase(rule.getMatchValue())) {
                lead.setAssignedTo(rule.getAssignTo());
                metrics.recordRoutingMatched(rule.getId().toString(), lead.getClientId());
                log.debug("LeadRouting: lead id={} assigned to '{}' via rule id={}", lead.getId(), rule.getAssignTo(), rule.getId());
                return;
            }
        }
    }

    private Map<String, String> buildFieldMap(Lead lead) {
        return Map.of(
                "businesstype",   nvl(lead.getBusinessType()),
                "customertype",   nvl(lead.getCustomerType()),
                "trafficsource",  nvl(lead.getTrafficSource()),
                "businessname",   nvl(lead.getBusinessName()),
                "email",          nvl(lead.getEmail())
        );
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
