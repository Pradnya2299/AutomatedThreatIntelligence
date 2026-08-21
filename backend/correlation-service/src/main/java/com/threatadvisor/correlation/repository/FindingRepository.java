package com.threatadvisor.correlation.repository;

import com.threatadvisor.correlation.domain.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    Optional<Finding> findByAssetIdAndVulnerabilityId(UUID assetId, UUID vulnerabilityId);

    List<Finding> findByVulnerabilityId(UUID vulnerabilityId);

    long countByVulnerabilityId(UUID vulnerabilityId);
}
