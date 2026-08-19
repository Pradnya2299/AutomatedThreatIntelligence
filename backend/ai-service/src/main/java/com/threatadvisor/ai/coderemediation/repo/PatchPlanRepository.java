package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.PatchPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatchPlanRepository extends JpaRepository<PatchPlanEntity, UUID> {
    Optional<PatchPlanEntity> findFirstByJobIdOrderByCreatedAtDesc(UUID jobId);
}
