package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CalendlyAccountRepository extends JpaRepository<CalendlyAccount, Long> {
    List<CalendlyAccount> findByPollEnabledTrue();
    Optional<CalendlyAccount> findByClientId(Long clientId);
}
