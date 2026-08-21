package com.threatadvisor.api.repository;

import com.threatadvisor.api.domain.RemediationPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RemediationPlanRepository extends JpaRepository<RemediationPlan, UUID> {
    List<RemediationPlan> findByFindingIdOrderByCreatedAtDesc(UUID findingId);
    Optional<RemediationPlan> findFirstByFindingIdOrderByCreatedAtDesc(UUID findingId);
}
