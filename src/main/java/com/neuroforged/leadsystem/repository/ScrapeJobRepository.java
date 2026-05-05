package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.ScrapeJob;
import com.neuroforged.leadsystem.entity.ScrapeJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, Long> {
    List<ScrapeJob> findByClientIdOrderByCreatedAtDesc(Long clientId);
    Optional<ScrapeJob> findByScraperJobId(String scraperJobId);
    List<ScrapeJob> findByStatusIn(List<ScrapeJobStatus> statuses);
}
