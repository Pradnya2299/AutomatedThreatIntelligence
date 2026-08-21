package com.threatadvisor.ai.kafka;

import com.threatadvisor.ai.service.RemediationGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RiskCalculatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(RiskCalculatedEventListener.class);

    private final RemediationGenerationService generationService;

    public RiskCalculatedEventListener(RemediationGenerationService generationService) {
        this.generationService = generationService;
    }

    @KafkaListener(topics = EventEnvelope.TYPE_RISK_CALCULATED, groupId = "ai-service")
    public void onRiskCalculated(String message) {
        log.info("operation=risk.calculated.received bytes={}", message == null ? 0 : message.length());
        try {
            generationService.handleRiskCalculated(message);
        } catch (RuntimeException ex) {
            log.error("operation=ai.consume.failed", ex);
        }
    }
}
