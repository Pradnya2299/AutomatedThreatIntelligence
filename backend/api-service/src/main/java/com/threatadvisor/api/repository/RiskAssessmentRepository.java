package com.threatadvisor.api.repository;

import com.threatadvisor.api.domain.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {
    Optional<RiskAssessment> findByFindingId(UUID findingId);
}
