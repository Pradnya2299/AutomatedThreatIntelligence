package com.threatadvisor.correlation.repository;

import com.threatadvisor.correlation.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
}
