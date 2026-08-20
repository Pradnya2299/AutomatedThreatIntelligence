package com.threatadvisor.ai.llm;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import com.threatadvisor.ai.domain.RemediationContext;
import com.threatadvisor.ai.dto.RemediationAiResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoAiRemediationServiceTest {

    private final DemoAiRemediationService demo = new DemoAiRemediationService();

    @Test
    void criticalInternetFacingIsImmediateAndLabeledDemo() {
        RemediationAiResponse response = demo.generate(context("CRITICAL", true));
        assertEquals("IMMEDIATE", response.getPriority());
        assertTrue(response.getSummary().startsWith("[DEMO MODE]"));
        assertTrue(response.getReasoning().contains("not from OpenAI"));
        assertTrue(response.getReasoning().contains("Emergency Security Patch Procedure"));
    }

    @Test
    void mediumInternalIsScheduled() {
        RemediationAiResponse response = demo.generate(context("MEDIUM", false));
        assertEquals("SCHEDULED", response.getPriority());
    }

    private static RemediationContext context(String level, boolean internet) {
        return new RemediationContext(
                "CVE-2021-44228", "Log4Shell", "CRITICAL", "10.0", "log4j", "2.14.1", null,
                "web-prod-01", "Linux", "PRODUCTION", "CRITICAL", internet, "log4j 2.14.1",
                "97.50", level, "cvss=100", "Risk is " + level,
                List.of(new KnowledgeChunkHit(UUID.randomUUID(), UUID.randomUUID(),
                        "Emergency Security Patch Procedure", "src", "EMERGENCY", "24 hours", 0.1)));
    }
}
