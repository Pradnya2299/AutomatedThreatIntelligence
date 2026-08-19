package com.threatadvisor.ingestion.repository;

import com.threatadvisor.ingestion.domain.NvdSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NvdSyncStateRepository extends JpaRepository<NvdSyncState, String> {
}
