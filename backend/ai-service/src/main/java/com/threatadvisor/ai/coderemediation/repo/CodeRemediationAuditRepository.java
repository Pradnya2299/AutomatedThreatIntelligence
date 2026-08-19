package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.CodeRemediationAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeRemediationAuditRepository extends JpaRepository<CodeRemediationAuditEntity, UUID> {
    List<CodeRemediationAuditEntity> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
