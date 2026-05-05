package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.LeadCommentDto;

import java.util.List;

public interface LeadCommentService {
    List<LeadCommentDto> getComments(Long leadId);
    LeadCommentDto addComment(Long leadId, String content, String authorEmail, String authorRole);
}
