package com.threatadvisor.risk.kafka;

import com.threatadvisor.risk.service.RiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FindingCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(FindingCreatedEventListener.class);

    private final RiskService riskService;

    public FindingCreatedEventListener(RiskService riskService) {
        this.riskService = riskService;
    }

    @KafkaListener(topics = EventEnvelope.TYPE_FINDING_CREATED, groupId = "risk-service")
    public void onFindingCreated(String message) {
        log.info("operation=finding.created.received bytes={}", message == null ? 0 : message.length());
        riskService.handleFindingCreated(message);
    }
}
