package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.*;
import com.neuroforged.leadsystem.entity.LeadStatus;
import com.neuroforged.leadsystem.repository.CalendlyMeetingRepository;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final CalendlyMeetingRepository meetingRepository;

    @Override
    public List<ClientSummaryDTO> getClientSummary() {
        return clientRepository.fetchClientSummaries().stream().map(r -> {
            long leads = r.getTotalLeads();
            long meetings = r.getTotalMeetings();
            return ClientSummaryDTO.builder()
                    .clientId(r.getClientId())
                    .clientName(r.getClientName())
                    .totalLeads(leads)
                    .totalMeetings(meetings)
                    .conversionRate(leads == 0 ? 0.0 : (double) meetings / leads)
                    .avgLeadScore(r.getAvgLeadScore())
                    .build();
        }).toList();
    }

    @Override
    public LeadKpiDTO getLeadKpis(Long clientId, String from, String to) {
        long leads = leadRepository.countFiltered(clientId, from, to);
        long meetings = clientId != null
                ? meetingRepository.findByClient_Id(clientId).size()
                : meetingRepository.count();
        double avgScore = leadRepository.avgLeadScore(clientId, from, to);
        long highQuality = leadRepository.countHighQuality(clientId, from, to);
        double convRate = leads == 0 ? 0.0 : (double) meetings / leads;
        return LeadKpiDTO.builder()
                .totalLeads(leads)
                .totalMeetings(meetings)
                .conversionRate(convRate)
                .avgLeadScore(avgScore)
                .highQualityLeads(highQuality)
                .build();
    }

    @Override
    public List<LeadVolumeDTO> getLeadVolume(Long clientId, String from, String to) {
        return leadRepository.findLeadVolumeByDate(clientId, from, to).stream()
                .map(r -> LeadVolumeDTO.builder()
                        .date(r.getDate())
                        .count(r.getCount())
                        .build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByTrafficSource(Long clientId) {
        return leadRepository.findCountsByTrafficSource(clientId).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByScoreBand(Long clientId) {
        return leadRepository.findCountsByScoreBand(clientId).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByBusinessType(Long clientId) {
        return leadRepository.findCountsByBusinessType(clientId).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByPipelineStatus(Long clientId) {
        return leadRepository.findCountsByStatus(clientId).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<TopLeadDTO> getTopLeads(Long clientId, int limit) {
        var pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "leadScore"));
        var page = clientId != null
                ? leadRepository.findByClient_Id(clientId, pageable)
                : leadRepository.findAll(pageable);
        return page.stream()
                .filter(l -> l.getStatus() != LeadStatus.BOOKED && l.getStatus() != LeadStatus.CLOSED)
                .filter(l -> l.getLeadScore() != null)
                .map(l -> TopLeadDTO.builder()
                        .id(l.getId())
                        .businessName(l.getBusinessName())
                        .email(l.getEmail())
                        .leadScore(l.getLeadScore())
                        .status(l.getStatus() != null ? l.getStatus().name() : null)
                        .clientId(l.getClientIdStr())
                        .createdAt(l.getCreatedAt())
                        .build())
                .toList();
    }
}
