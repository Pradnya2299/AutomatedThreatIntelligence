package com.threatadvisor.ai.coderemediation.repo;

import com.threatadvisor.ai.coderemediation.domain.RepositoryBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepositoryBindingRepository extends JpaRepository<RepositoryBindingEntity, UUID> {
    List<RepositoryBindingEntity> findByHostnameIgnoreCase(String hostname);

    List<RepositoryBindingEntity> findByAssetId(UUID assetId);
}
