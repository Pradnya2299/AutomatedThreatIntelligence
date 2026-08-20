package com.threatadvisor.correlation.kafka;

import com.threatadvisor.correlation.service.CorrelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CveNormalizedEventListener {

    private static final Logger log = LoggerFactory.getLogger(CveNormalizedEventListener.class);

    private final CorrelationService correlationService;

    public CveNormalizedEventListener(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    @KafkaListener(topics = EventEnvelope.TYPE_NORMALIZED, groupId = "correlation-service")
    public void onCveNormalized(String message) {
        log.info("operation=cve.normalized.received bytes={}", message == null ? 0 : message.length());
        correlationService.handleNormalizedEvent(message);
    }
}
