package com.threatadvisor.api.repository;

import com.threatadvisor.api.domain.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID> {
    List<Finding> findByVulnerabilityId(UUID vulnerabilityId);
    List<Finding> findByAssetId(UUID assetId);
}
