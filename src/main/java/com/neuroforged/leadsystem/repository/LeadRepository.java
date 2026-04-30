package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    boolean existsByEmailAndClientId(String email, String clientId);

    boolean existsByBusinessName(String businessName);

    Optional<Lead> findByEmail(String email);

    Optional<Lead> findByBusinessName(String businessName);


    List<Lead> findByClientId(String clientId);
}