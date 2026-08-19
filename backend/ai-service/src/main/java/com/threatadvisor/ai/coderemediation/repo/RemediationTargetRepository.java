package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.RemediationTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RemediationTargetRepository extends JpaRepository<RemediationTargetEntity, UUID> {
    Optional<RemediationTargetEntity> findFirstByJobId(UUID jobId);
}
