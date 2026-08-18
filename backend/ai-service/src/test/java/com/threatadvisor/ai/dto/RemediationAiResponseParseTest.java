package com.threatadvisor.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemediationAiResponseParseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesStructuredJson() throws Exception {
        String json = """
                {
                  "summary": "Emergency patch",
                  "priority": "IMMEDIATE",
                  "recommendedAction": "Apply vendor patch",
                  "targetVersion": "unavailable in supplied facts",
                  "affectedComponents": ["httpd"],
                  "prerequisites": ["change ticket"],
                  "implementationSteps": ["patch"],
                  "validationSteps": ["health check"],
                  "rollbackPlan": "restore previous package",
                  "downtimeExpected": true,
                  "reasoning": "Follow retrieved emergency procedure",
                  "references": ["Emergency Security Patch Procedure"]
                }
                """;
        RemediationAiResponse parsed = mapper.readValue(json, RemediationAiResponse.class);
        assertEquals("IMMEDIATE", parsed.getPriority());
        assertEquals("Apply vendor patch", parsed.getRecommendedAction());
        assertTrue(parsed.getDowntimeExpected());
        assertEquals(1, parsed.getValidationSteps().size());
    }
}
