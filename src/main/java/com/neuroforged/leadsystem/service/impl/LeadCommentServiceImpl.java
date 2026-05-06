package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.LeadCommentDto;
import com.neuroforged.leadsystem.entity.LeadComment;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.LeadCommentRepository;
import com.neuroforged.leadsystem.repository.LeadRepository;
import com.neuroforged.leadsystem.service.LeadCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadCommentServiceImpl implements LeadCommentService {

    private final LeadCommentRepository leadCommentRepository;
    private final LeadRepository leadRepository;

    @Override
    public List<LeadCommentDto> getComments(Long leadId) {
        if (!leadRepository.existsById(leadId)) {
            throw new ResourceNotFoundException("Lead not found: " + leadId);
        }
        return leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(leadId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public LeadCommentDto addComment(Long leadId, String content, String authorEmail, String authorRole) {
        if (!leadRepository.existsById(leadId)) {
            throw new ResourceNotFoundException("Lead not found: " + leadId);
        }
        LeadComment comment = LeadComment.builder()
                .leadId(leadId)
                .content(content)
                .authorEmail(authorEmail)
                .authorRole(authorRole)
                .build();
        return toDto(leadCommentRepository.save(comment));
    }

    private LeadCommentDto toDto(LeadComment c) {
        return LeadCommentDto.builder()
                .id(c.getId())
                .leadId(c.getLeadId())
                .authorEmail(c.getAuthorEmail())
                .authorRole(c.getAuthorRole())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
