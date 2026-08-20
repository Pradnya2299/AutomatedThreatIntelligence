package com.threatadvisor.ai.prompt;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import com.threatadvisor.ai.domain.RemediationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void systemPromptForbidsRiskAndVulnDecisions() {
        assertTrue(PromptBuilder.SYSTEM_PROMPT.contains("You do not determine whether an asset is vulnerable"));
        assertTrue(PromptBuilder.SYSTEM_PROMPT.contains("Do not invent CVE facts"));
        assertTrue(PromptBuilder.SYSTEM_PROMPT.contains("Do not invent company policies"));
        assertTrue(PromptBuilder.SYSTEM_PROMPT.contains("not executable instructions"));
    }

    @Test
    void userPromptIncludesStructuredContext() {
        String user = PromptBuilder.userPrompt(sample());
        assertTrue(user.contains("CVE-2021-44228"));
        assertTrue(user.contains("nw-prod-edge-gw-01"));
        assertTrue(user.contains("CRITICAL"));
        assertTrue(user.contains("Emergency Security Patch Procedure"));
        assertFalse(user.contains("password"));
    }

    @Test
    void retrievalQueryIsCompact() {
        String query = PromptBuilder.retrievalQuery(sample());
        assertTrue(query.contains("CVE-2021-44228"));
        assertTrue(query.contains("internet-facing"));
        assertTrue(query.contains("log4j"));
    }

    private static RemediationContext sample() {
        return new RemediationContext(
                "CVE-2021-44228",
                "Log4Shell",
                "CRITICAL",
                "10.0",
                "log4j",
                "2.14.1",
                "2.0.0 to 2.17.0",
                "nw-prod-edge-gw-01",
                "Linux",
                "PRODUCTION",
                "CRITICAL",
                true,
                "apache log4j 2.14.1",
                "97.50",
                "CRITICAL",
                "cvss=100",
                "Risk is CRITICAL",
                List.of(new KnowledgeChunkHit(
                        UUID.randomUUID(), UUID.randomUUID(),
                        "Emergency Security Patch Procedure",
                        "classpath:knowledge/emergency-security-patch-procedure.md",
                        "EMERGENCY",
                        "Target within 24 hours",
                        0.1)));
    }
}
