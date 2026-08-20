package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.CodeRemediationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeRemediationJobRepository extends JpaRepository<CodeRemediationJobEntity, UUID> {
    List<CodeRemediationJobEntity> findByInvestigationIdOrderByCreatedAtDesc(UUID investigationId);

    Optional<CodeRemediationJobEntity> findFirstByInvestigationIdOrderByCreatedAtDesc(UUID investigationId);
}
