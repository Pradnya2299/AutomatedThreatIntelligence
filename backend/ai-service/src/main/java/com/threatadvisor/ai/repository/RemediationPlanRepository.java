package com.threatadvisor.ai.repository;

import com.threatadvisor.ai.domain.RemediationPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RemediationPlanRepository extends JpaRepository<RemediationPlan, UUID> {
    Optional<RemediationPlan> findByFindingIdAndRiskAssessmentId(UUID findingId, UUID riskAssessmentId);
    long countByFindingIdAndRiskAssessmentId(UUID findingId, UUID riskAssessmentId);
}
