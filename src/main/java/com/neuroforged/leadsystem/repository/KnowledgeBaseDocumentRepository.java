package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.KnowledgeBaseDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeBaseDocumentRepository extends JpaRepository<KnowledgeBaseDocument, Long> {
    List<KnowledgeBaseDocument> findByClientIdOrderByFilenameAsc(Long clientId);
    void deleteByClientId(Long clientId);

    @Query("SELECT d FROM KnowledgeBaseDocument d WHERE d.client.id = :clientId AND LOWER(d.content) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<KnowledgeBaseDocument> searchByClientId(@Param("clientId") Long clientId, @Param("q") String q);
}
