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
        String cid = clientId != null ? String.valueOf(clientId) : null;
        long leads = leadRepository.countFiltered(cid, from, to);
        long meetings = clientId != null
                ? meetingRepository.findByClient_Id(clientId).size()
                : meetingRepository.count();
        double avgScore = leadRepository.avgLeadScore(cid, from, to);
        long highQuality = leadRepository.countHighQuality(cid, from, to);
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
        String cid = clientId != null ? String.valueOf(clientId) : null;
        return leadRepository.findLeadVolumeByDate(cid, from, to).stream()
                .map(r -> LeadVolumeDTO.builder()
                        .date(r.getDate())
                        .count(r.getCount())
                        .build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByTrafficSource(Long clientId) {
        String cid = clientId != null ? String.valueOf(clientId) : null;
        return leadRepository.findCountsByTrafficSource(cid).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByScoreBand(Long clientId) {
        String cid = clientId != null ? String.valueOf(clientId) : null;
        return leadRepository.findCountsByScoreBand(cid).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByBusinessType(Long clientId) {
        String cid = clientId != null ? String.valueOf(clientId) : null;
        return leadRepository.findCountsByBusinessType(cid).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<GroupCountDTO> getLeadsByPipelineStatus(Long clientId) {
        String cid = clientId != null ? String.valueOf(clientId) : null;
        return leadRepository.findCountsByStatus(cid).stream()
                .map(r -> GroupCountDTO.builder().label(r.getLabel()).count(r.getCount()).build())
                .toList();
    }

    @Override
    public List<TopLeadDTO> getTopLeads(Long clientId, int limit) {
        String cid = clientId != null ? String.valueOf(clientId) : null;
        var pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "leadScore"));
        var page = cid != null
                ? leadRepository.findByClientId(cid, pageable)
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
                        .clientId(l.getClientId())
                        .createdAt(l.getCreatedAt())
                        .build())
                .toList();
    }
}
