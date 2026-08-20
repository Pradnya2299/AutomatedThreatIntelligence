package com.threatadvisor.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.dto.RemediationAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Optional Redis cache of structured AI output. PostgreSQL remains the source of truth.
 */
@Component
public class RemediationResponseCache {

    private static final Logger log = LoggerFactory.getLogger(RemediationResponseCache.class);

    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final StringRedisTemplate redis;

    public RemediationResponseCache(
            ObjectMapper objectMapper,
            AiProperties properties,
            @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redis) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.redis = redis;
    }

    public RemediationAiResponse get(UUID findingId, UUID riskId) {
        if (redis == null) {
            return null;
        }
        try {
            String raw = redis.opsForValue().get(key(findingId, riskId));
            if (raw == null) {
                return null;
            }
            return objectMapper.readValue(raw, RemediationAiResponse.class);
        } catch (Exception ex) {
            log.warn("operation=ai.cache.unavailable");
            return null;
        }
    }

    public void put(UUID findingId, UUID riskId, RemediationAiResponse response) {
        if (redis == null || response == null) {
            return;
        }
        try {
            redis.opsForValue().set(
                    key(findingId, riskId),
                    objectMapper.writeValueAsString(response),
                    Duration.ofSeconds(properties.getCacheTtlSeconds()));
        } catch (Exception ex) {
            log.warn("operation=ai.cache.write-skipped");
        }
    }

    static String key(UUID findingId, UUID riskId) {
        return "ai:remediation:" + findingId + ":" + riskId;
    }
}
