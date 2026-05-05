package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.LeadComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadCommentRepository extends JpaRepository<LeadComment, Long> {

    List<LeadComment> findByLeadIdOrderByCreatedAtAsc(Long leadId);
}
