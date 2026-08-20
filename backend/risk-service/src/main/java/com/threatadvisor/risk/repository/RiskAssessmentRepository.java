package com.threatadvisor.risk.repository;

import com.threatadvisor.risk.domain.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findByFindingId(UUID findingId);

    long countByFindingId(UUID findingId);
}
