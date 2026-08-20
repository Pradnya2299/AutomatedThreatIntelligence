package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.RiskEngineSnapshot;

import java.util.UUID;

public interface RiskCalculationTool {
    RiskEngineSnapshot calculate(UUID findingId, UUID correlationId);
}
