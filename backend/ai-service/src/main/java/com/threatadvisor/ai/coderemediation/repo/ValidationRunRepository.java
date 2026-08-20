package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.ValidationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValidationRunRepository extends JpaRepository<ValidationRunEntity, UUID> {
    List<ValidationRunEntity> findByPatchExecutionIdOrderByCreatedAtAsc(UUID patchExecutionId);
}
