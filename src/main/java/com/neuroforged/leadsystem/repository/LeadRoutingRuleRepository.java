package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.LeadRoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRoutingRuleRepository extends JpaRepository<LeadRoutingRule, Long> {

    List<LeadRoutingRule> findByClientIdOrderByPriorityAscIdAsc(Long clientId);
}
