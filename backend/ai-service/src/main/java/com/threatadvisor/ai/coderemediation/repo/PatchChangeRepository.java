package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.PatchChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatchChangeRepository extends JpaRepository<PatchChangeEntity, UUID> {
    List<PatchChangeEntity> findByPatchExecutionId(UUID patchExecutionId);
}
