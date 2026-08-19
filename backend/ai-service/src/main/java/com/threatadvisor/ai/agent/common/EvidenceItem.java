package com.threatadvisor.ai.agent.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvidenceItem(
        String source,
        String type,
        String detail,
        String description,
        String value,
        Confidence confidence,
        Instant timestamp,
        EvidenceAuthority authority
) {
    public EvidenceItem(String source, String type, String detail) {
        this(source, type, detail, detail, detail, Confidence.HIGH, Instant.now(), EvidenceAuthority.DETERMINISTIC);
    }

    public static EvidenceItem fact(
            EvidenceSource source, String type, String description, String value, Confidence confidence) {
        return new EvidenceItem(
                source.name(),
                type,
                description,
                description,
                value,
                confidence,
                Instant.now(),
                EvidenceAuthority.DETERMINISTIC);
    }

    public static EvidenceItem interpretation(String description, String value) {
        return new EvidenceItem(
                EvidenceSource.LLM.name(),
                "interpretation",
                description,
                description,
                value,
                Confidence.LOW,
                Instant.now(),
                EvidenceAuthority.INTERPRETATION);
    }
}
