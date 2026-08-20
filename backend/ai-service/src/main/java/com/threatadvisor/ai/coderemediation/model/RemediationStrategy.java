package com.threatadvisor.ai.coderemediation.model;

import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.coderemediation.RemediationStrategyType;

import java.util.List;

public record RemediationStrategy(
        RemediationStrategyType strategyType,
        String rationale,
        List<String> affectedFiles,
        List<String> expectedChanges,
        Confidence confidence,
        List<String> prerequisites,
        List<String> risks,
        String rollbackPlan
) {
}
