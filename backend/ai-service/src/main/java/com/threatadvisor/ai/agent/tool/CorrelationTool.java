package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.CorrelationToolResult;

import java.util.UUID;

public interface CorrelationTool {
    CorrelationToolResult correlate(String cveId, UUID correlationId);
}
