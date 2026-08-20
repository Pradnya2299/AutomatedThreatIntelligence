package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.SecurityVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecurityVerificationRepository extends JpaRepository<SecurityVerificationEntity, UUID> {
    Optional<SecurityVerificationEntity> findFirstByPatchExecutionIdOrderByCreatedAtDesc(UUID patchExecutionId);
}
