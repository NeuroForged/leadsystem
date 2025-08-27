package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.List;

public interface CalendlyAccountRe
        List<CalendlyAccount> findByPollEnabledTrue();
pository extends JpaRepository<CalendlyAccount, Long> {
    Optional<CalendlyAccount> findByClientId(Long clientId);
}
