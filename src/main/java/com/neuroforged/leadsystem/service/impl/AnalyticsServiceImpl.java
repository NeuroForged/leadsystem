package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.*;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final CalendlyMeetingRepository meetingRepository;

    @Override
    public List<ClientSummaryDTO> getClientSummary() {
        return clientRepository.findAll().stream().map(client -> {
            String clientIdStr = String.valueOf(client.getId());
            List<Lead> leads = leadRepository.findByClientId(clientIdStr);
            long meetingCount = meetingRepository.findByClient_Id(client.getId()).size();
            double avgScore = leads.stream()
                    .filter(l -> l.getLeadScore() != null)
                    .mapToInt(Lead::getLeadScore)
                    .average()
                    .orElse(0.0);
            double convRate = leads.isEmpty() ? 0.0 : (double) meetingCount / leads.size();
            return ClientSummaryDTO.builder()
                    .clientId(client.getId())
                    .clientName(client.getName())
                    .totalLeads(leads.size())
                    .totalMeetings(meetingCount)
                    .conversionRate(convRate)
                    .avgLeadScore(avgScore)
                    .build();
        }).toList();
    }

    @Override
    public LeadKpiDTO getLeadKpis(Long clientId, String from, String to) {
        List<Lead> leads = getFilteredLeads(clientId, from, to);
        long meetingCount = clientId != null
                ? meetingRepository.findByClient_Id(clientId).size()
                : meetingRepository.count();
        double avgScore = leads.stream()
                .filter(l -> l.getLeadScore() != null)
                .mapToInt(Lead::getLeadScore)
                .average()
                .orElse(0.0);
        long highQuality = leads.stream()
                .filter(l -> l.getLeadScore() != null && l.getLeadScore() >= 80)
                .count();
        double convRate = leads.isEmpty() ? 0.0 : (double) meetingCount / leads.size();
        return LeadKpiDTO.builder()
                .totalLeads(leads.size())
                .totalMeetings(meetingCount)
                .conversionRate(convRate)
                .avgLeadScore(avgScore)
                .highQualityLeads(highQuality)
                .build();
    }

    @Override
    public List<LeadVolumeDTO> getLeadVolume(Long clientId, String from, String to) {
        List<Lead> leads = getFilteredLeads(clientId, from, to);
        return leads.stream()
                .filter(l -> l.getCreatedAt() != null)
                .collect(Collectors.groupingBy(l -> l.getCreatedAt().toLocalDate(), TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(e -> LeadVolumeDTO.builder()
                        .date(e.getKey().toString())
                        .count(e.getValue())
                        .build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByTrafficSource(Long clientId) {
        List<Lead> leads = getFilteredLeads(clientId, null, null);
        return leads.stream()
                .filter(l -> l.getTrafficSource() != null)
                .collect(Collectors.groupingBy(Lead::getTrafficSource, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> GroupCountDTO.builder().label(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByScoreBand(Long clientId) {
        List<Lead> leads = getFilteredLeads(clientId, null, null);
        Map<String, Long> bands = new LinkedHashMap<>();
        bands.put("High", 0L);
        bands.put("Medium", 0L);
        bands.put("Low", 0L);
        for (Lead l : leads) {
            if (l.getLeadScore() == null) continue;
            int score = l.getLeadScore();
            if (score >= 80) bands.merge("High", 1L, Long::sum);
            else if (score >= 60) bands.merge("Medium", 1L, Long::sum);
            else bands.merge("Low", 1L, Long::sum);
        }
        return bands.entrySet().stream()
                .map(e -> GroupCountDTO.builder().label(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByBusinessType(Long clientId) {
        List<Lead> leads = getFilteredLeads(clientId, null, null);
        return leads.stream()
                .filter(l -> l.getBusinessType() != null)
                .collect(Collectors.groupingBy(Lead::getBusinessType, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> GroupCountDTO.builder().label(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByPipelineStatus(Long clientId) {
        List<Lead> leads = getFilteredLeads(clientId, null, null);
        return leads.stream()
                .filter(l -> l.getStatus() != null)
                .collect(Collectors.groupingBy(l -> l.getStatus().name(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> GroupCountDTO.builder().label(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    @Override
    public List<TopLeadDTO> getTopLeads(Long clientId, int limit) {
        List<Lead> leads = getFilteredLeads(clientId, null, null);
        return leads.stream()
                .filter(l -> l.getStatus() != LeadStatus.BOOKED && l.getStatus() != LeadStatus.CLOSED)
                .filter(l -> l.getLeadScore() != null)
                .sorted(Comparator.comparingInt(Lead::getLeadScore).reversed())
                .limit(limit)
                .map(l -> TopLeadDTO.builder()
                        .id(l.getId())
                        .businessName(l.getBusinessName())
                        .email(l.getEmail())
                        .leadScore(l.getLeadScore())
                        .status(l.getStatus() != null ? l.getStatus().name() : null)
                        .clientId(l.getClientId())
                        .createdAt(l.getCreatedAt())
                        .build())
                .toList();
    }

    private List<Lead> getFilteredLeads(Long clientId, String from, String to) {
        List<Lead> leads = clientId != null
                ? leadRepository.findByClientId(String.valueOf(clientId))
                : leadRepository.findAll();

        if (from != null || to != null) {
            LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
            LocalDate toDate = to != null ? LocalDate.parse(to) : null;
            leads = leads.stream()
                    .filter(l -> {
                        if (l.getCreatedAt() == null) return false;
                        LocalDate date = l.getCreatedAt().toLocalDate();
                        if (fromDate != null && date.isBefore(fromDate)) return false;
                        if (toDate != null && date.isAfter(toDate)) return false;
                        return true;
                    })
                    .toList();
        }
        return leads;
    }
}
