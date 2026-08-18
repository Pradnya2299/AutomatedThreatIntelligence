package com.threatadvisor.risk.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RiskEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RiskEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String key, EventEnvelope envelope) {
        try {
            MDC.put("eventId", envelope.eventId().toString());
            if (envelope.correlationId() != null) {
                MDC.put("correlationId", envelope.correlationId().toString());
            }
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(topic, key, json).get();
            log.info("operation=kafka.publish topic={} key={} eventType={}", topic, key, envelope.eventType());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka envelope", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish Kafka event to " + topic, ex);
        }
    }
}
