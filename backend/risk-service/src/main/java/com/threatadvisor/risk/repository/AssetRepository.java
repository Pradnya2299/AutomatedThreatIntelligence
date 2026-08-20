package com.threatadvisor.risk.repository;

import com.threatadvisor.risk.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
}
