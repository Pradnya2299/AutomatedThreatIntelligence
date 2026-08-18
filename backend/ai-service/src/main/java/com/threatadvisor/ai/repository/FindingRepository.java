package com.threatadvisor.ai.repository;

import com.threatadvisor.ai.domain.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID> {
}
