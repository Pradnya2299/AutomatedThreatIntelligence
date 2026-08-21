package com.threatadvisor.ai.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemediationAiResponseValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCompletePlan() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void rejectsBadPriorityAndMissingSummary() {
        RemediationAiResponse invalid = valid();
        invalid.setPriority("SOON");
        invalid.setSummary(" ");
        assertFalse(validator.validate(invalid).isEmpty());
    }

    private static RemediationAiResponse valid() {
        RemediationAiResponse response = new RemediationAiResponse();
        response.setSummary("Patch now");
        response.setPriority("IMMEDIATE");
        response.setRecommendedAction("Apply vendor patch");
        response.setTargetVersion("unavailable");
        response.setAffectedComponents(List.of("log4j"));
        response.setPrerequisites(List.of("backup"));
        response.setImplementationSteps(List.of("patch"));
        response.setValidationSteps(List.of("health"));
        response.setRollbackPlan("restore previous package");
        response.setDowntimeExpected(true);
        response.setReasoning("policy requires 24h");
        response.setReferences(List.of("Emergency Security Patch Procedure"));
        return response;
    }
}
