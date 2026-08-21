package com.threatadvisor.api.repository;

import com.threatadvisor.api.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
}
