package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.PatchExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatchExecutionRepository extends JpaRepository<PatchExecutionEntity, UUID> {
    List<PatchExecutionEntity> findByJobIdOrderByAttemptNumberAsc(UUID jobId);

    Optional<PatchExecutionEntity> findFirstByJobIdOrderByAttemptNumberDesc(UUID jobId);
}
