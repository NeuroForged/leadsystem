package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendlyAccountRepository extends JpaRepository<CalendlyAccount, Long> {
    Optional<CalendlyAccount> findByClientId(Long clientId);
    long countByRequiresReauthTrue();
    List<CalendlyAccount> findAllByUsePollingTrueAndRequiresReauthFalse();
}
