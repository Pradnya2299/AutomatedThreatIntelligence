package com.threatadvisor.ai.repository;

import com.threatadvisor.ai.domain.SecurityInvestigation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityInvestigationRepository extends JpaRepository<SecurityInvestigation, UUID> {
}
