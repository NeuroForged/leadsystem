package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.CreateLeadRoutingRuleRequest;
import com.neuroforged.leadsystem.dto.LeadRoutingRuleDto;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.LeadRoutingRule;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRoutingRuleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients/{clientId}/routing-rules")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LeadRoutingRuleController {

    private final LeadRoutingRuleRepository ruleRepository;
    private final ClientRepository clientRepository;

    @GetMapping
    public List<LeadRoutingRuleDto> list(@PathVariable Long clientId) {
        return ruleRepository.findByClientIdOrderByPriorityAscIdAsc(clientId)
                .stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadRoutingRuleDto create(@PathVariable Long clientId,
                                     @Valid @RequestBody CreateLeadRoutingRuleRequest req) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        LeadRoutingRule rule = LeadRoutingRule.builder()
                .client(client)
                .matchField(req.getMatchField())
                .matchValue(req.getMatchValue())
                .assignTo(req.getAssignTo())
                .priority(req.getPriority() != null ? req.getPriority() : 100)
                .build();

        return toDto(ruleRepository.save(rule));
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long clientId, @PathVariable Long ruleId) {
        LeadRoutingRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule not found: " + ruleId));
        if (!rule.getClient().getId().equals(clientId)) {
            throw new ResourceNotFoundException("Routing rule not found: " + ruleId);
        }
        ruleRepository.delete(rule);
    }

    private LeadRoutingRuleDto toDto(LeadRoutingRule r) {
        LeadRoutingRuleDto dto = new LeadRoutingRuleDto();
        dto.setId(r.getId());
        dto.setClientId(r.getClient().getId());
        dto.setMatchField(r.getMatchField());
        dto.setMatchValue(r.getMatchValue());
        dto.setAssignTo(r.getAssignTo());
        dto.setPriority(r.getPriority());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
