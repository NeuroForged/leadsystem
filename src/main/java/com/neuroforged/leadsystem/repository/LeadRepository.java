package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    boolean existsByEmail(String email);

    boolean existsByBusinessName(String businessName);

    Optional<Lead> findByEmail(String email);

    Optional<Lead> findByBusinessName(String businessName);


    List<Lead> findByClientId(String clientId);
}