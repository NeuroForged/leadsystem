package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadRoutingRule;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.LeadRoutingRuleRepository;
import com.neuroforged.leadsystem.service.impl.LeadRoutingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadRoutingServiceImplTest {

    @Mock
    private LeadRoutingRuleRepository routingRuleRepository;

    @Mock
    private LeadSystemMetrics metrics;

    @InjectMocks
    private LeadRoutingServiceImpl routingService;

    private Lead buildLead(String clientIdStr, String businessType) {
        return Lead.builder()
                .clientIdStr(clientIdStr)
                .businessType(businessType)
                .build();
    }

    private LeadRoutingRule buildRule(Long id, String field, String value, String assignTo, int priority) {
        return LeadRoutingRule.builder()
                .id(id)
                .matchField(field)
                .matchValue(value)
                .assignTo(assignTo)
                .priority(priority)
                .build();
    }

    @Test
    void route_noRules_doesNotAssign() {
        Lead lead = buildLead("1", "SaaS");
        when(routingRuleRepository.findByClientIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of());

        routingService.route(lead);

        assertThat(lead.getAssignedTo()).isNull();
    }

    @Test
    void route_matchingRule_assignsCorrectly() {
        Lead lead = buildLead("1", "SaaS");
        LeadRoutingRule rule = buildRule(10L, "businessType", "SaaS", "alice@example.com", 100);
        when(routingRuleRepository.findByClientIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(rule));

        routingService.route(lead);

        assertThat(lead.getAssignedTo()).isEqualTo("alice@example.com");
        verify(metrics).recordRoutingMatched("10", "1");
    }

    @Test
    void route_noMatchingRule_doesNotAssign() {
        Lead lead = buildLead("1", "eCommerce");
        LeadRoutingRule rule = buildRule(10L, "businessType", "SaaS", "alice@example.com", 100);
        when(routingRuleRepository.findByClientIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(rule));

        routingService.route(lead);

        assertThat(lead.getAssignedTo()).isNull();
        verify(metrics, never()).recordRoutingMatched(any(), any());
    }

    @Test
    void route_multipleRules_firstMatchWins() {
        Lead lead = buildLead("1", "SaaS");
        lead.setCustomerType("B2B");

        // priority 50 matches businessType, priority 100 matches customerType — lower wins
        LeadRoutingRule highPriority = buildRule(1L, "businessType", "SaaS", "first@example.com", 50);
        LeadRoutingRule lowPriority  = buildRule(2L, "customerType", "B2B", "second@example.com", 100);

        when(routingRuleRepository.findByClientIdOrderByPriorityAscIdAsc(1L))
                .thenReturn(List.of(highPriority, lowPriority));

        routingService.route(lead);

        assertThat(lead.getAssignedTo()).isEqualTo("first@example.com");
        verify(metrics).recordRoutingMatched("1", "1");
    }

    @Test
    void route_unparsableClientId_skips() {
        Lead lead = buildLead("not-a-number", "SaaS");

        routingService.route(lead);

        verify(routingRuleRepository, never()).findByClientIdOrderByPriorityAscIdAsc(anyLong());
        assertThat(lead.getAssignedTo()).isNull();
    }

    @Test
    void route_caseInsensitiveMatch() {
        Lead lead = buildLead("1", "SaaS");
        LeadRoutingRule rule = buildRule(10L, "businessType", "saas", "bob@example.com", 100);
        when(routingRuleRepository.findByClientIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(rule));

        routingService.route(lead);

        assertThat(lead.getAssignedTo()).isEqualTo("bob@example.com");
    }
}
