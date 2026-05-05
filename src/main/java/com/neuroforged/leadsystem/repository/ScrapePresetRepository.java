package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.ScrapePreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapePresetRepository extends JpaRepository<ScrapePreset, Long> {
    List<ScrapePreset> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
