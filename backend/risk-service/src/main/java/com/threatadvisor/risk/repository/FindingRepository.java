package com.threatadvisor.risk.repository;

import com.threatadvisor.risk.domain.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID> {
}
