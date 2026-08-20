package com.threatadvisor.ingestion.repository;

import com.threatadvisor.ingestion.domain.EventProcessingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventProcessingRecordRepository extends JpaRepository<EventProcessingRecord, UUID> {

    boolean existsByEventIdAndConsumer(UUID eventId, String consumer);
}
