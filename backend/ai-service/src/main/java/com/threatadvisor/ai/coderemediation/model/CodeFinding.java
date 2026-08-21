package com.threatadvisor.ai.coderemediation.model;

import com.threatadvisor.ai.agent.common.Confidence;

public record CodeFinding(
        String file,
        Integer line,
        String component,
        String currentValue,
        String expectedValue,
        String vulnerabilityRelation,
        Confidence confidence,
        String evidence
) {
}
