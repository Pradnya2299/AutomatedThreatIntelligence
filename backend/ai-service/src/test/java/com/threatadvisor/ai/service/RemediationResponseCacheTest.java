package com.threatadvisor.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.dto.RemediationAiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RemediationResponseCacheTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AiProperties properties = new AiProperties();

    @Test
    void returnsNullWhenRedisMissing() {
        RemediationResponseCache cache = new RemediationResponseCache(mapper, properties, null);
        assertNull(cache.get(UUID.randomUUID(), UUID.randomUUID()));
        cache.put(UUID.randomUUID(), UUID.randomUUID(), sample());
    }

    @Test
    void roundTripsJson() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        UUID finding = UUID.randomUUID();
        UUID risk = UUID.randomUUID();
        when(values.get(RemediationResponseCache.key(finding, risk)))
                .thenReturn(mapper.writeValueAsString(sample()));
        RemediationResponseCache cache = new RemediationResponseCache(mapper, properties, redis);
        RemediationAiResponse loaded = cache.get(finding, risk);
        assertNotNull(loaded);
        assertEquals("IMMEDIATE", loaded.getPriority());
    }

    @Test
    void redisFailureIsIgnored() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));
        RemediationResponseCache cache = new RemediationResponseCache(mapper, properties, redis);
        assertNull(cache.get(UUID.randomUUID(), UUID.randomUUID()));
        cache.put(UUID.randomUUID(), UUID.randomUUID(), sample());
    }

    @Test
    void putIsSkippedWhenRedisThrowsOnSet() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new RuntimeException("timeout")).when(values).set(anyString(), anyString(), any());
        RemediationResponseCache cache = new RemediationResponseCache(mapper, properties, redis);
        cache.put(UUID.randomUUID(), UUID.randomUUID(), sample());
    }

    private static RemediationAiResponse sample() {
        RemediationAiResponse response = new RemediationAiResponse();
        response.setSummary("[DEMO MODE] test");
        response.setPriority("IMMEDIATE");
        response.setRecommendedAction("patch");
        response.setTargetVersion("unavailable");
        response.setAffectedComponents(List.of("httpd"));
        response.setPrerequisites(List.of("backup"));
        response.setImplementationSteps(List.of("apply"));
        response.setValidationSteps(List.of("health"));
        response.setRollbackPlan("restore previous package");
        response.setDowntimeExpected(true);
        response.setReasoning("policy");
        response.setReferences(List.of("Emergency Security Patch Procedure"));
        return response;
    }
}
