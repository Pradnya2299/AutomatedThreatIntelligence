package com.threatadvisor.ingestion.kafka;

import com.threatadvisor.ingestion.service.CveNormalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CveRawEventListener {

    private static final Logger log = LoggerFactory.getLogger(CveRawEventListener.class);

    private final CveNormalizationService normalizationService;

    public CveRawEventListener(CveNormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    @KafkaListener(topics = EventEnvelope.TYPE_RAW, groupId = "ingestion-service")
    public void onCveRaw(String message) {
        log.info("operation=cve.raw.received bytes={}", message == null ? 0 : message.length());
        normalizationService.handleRawEvent(message);
    }
}
