package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByClientId(String clientId);
}