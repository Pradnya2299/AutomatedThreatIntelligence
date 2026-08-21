package com.threatadvisor.ingestion.repository;

import com.threatadvisor.ingestion.domain.CveRawRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CveRawRecordRepository extends JpaRepository<CveRawRecord, UUID> {

    Optional<CveRawRecord> findBySourceAndExternalId(String source, String externalId);

    Optional<CveRawRecord> findByEventId(UUID eventId);
}
