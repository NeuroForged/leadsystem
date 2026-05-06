package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByActorEmail(String actorEmail, Pageable pageable);

    Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
}
