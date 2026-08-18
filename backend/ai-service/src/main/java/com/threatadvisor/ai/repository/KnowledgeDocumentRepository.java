package com.threatadvisor.ai.repository;

import com.threatadvisor.ai.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    Optional<KnowledgeDocument> findBySource(String source);
}
