package com.threatadvisor.ai.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RemediationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RemediationEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RemediationEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String key, EventEnvelope envelope) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(envelope)).get();
            log.info("operation=kafka.publish topic={} eventType={}", topic, envelope.eventType());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize Kafka envelope", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish Kafka event to " + topic, ex);
        }
    }
}
