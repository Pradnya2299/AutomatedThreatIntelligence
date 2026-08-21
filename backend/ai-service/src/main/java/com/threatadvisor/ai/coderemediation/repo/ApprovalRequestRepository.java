package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.ApprovalRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    Optional<ApprovalRequestEntity> findFirstByJobIdOrderByCreatedAtDesc(UUID jobId);
}
