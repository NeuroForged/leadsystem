package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CalendlyAccountRepository extends JpaRepository<CalendlyAccount, Long> {
    Optional<CalendlyAccount> findByClientId(Long clientId);
    List<CalendlyAccount> findAllByClientIdIn(Collection<Long> clientIds);
    long countByRequiresReauthTrue();
    List<CalendlyAccount> findAllByUsePollingTrueAndRequiresReauthFalse();
}
