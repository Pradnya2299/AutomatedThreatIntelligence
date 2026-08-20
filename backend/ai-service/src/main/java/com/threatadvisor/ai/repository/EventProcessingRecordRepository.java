package com.threatadvisor.ai.repository;

import com.threatadvisor.ai.domain.EventProcessingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventProcessingRecordRepository extends JpaRepository<EventProcessingRecord, UUID> {
    boolean existsByEventIdAndConsumer(UUID eventId, String consumer);
}
