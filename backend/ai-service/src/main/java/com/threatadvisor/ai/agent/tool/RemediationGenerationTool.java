package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.RemediationToolResult;

import java.util.UUID;

public interface RemediationGenerationTool {
    RemediationToolResult generate(UUID findingId, UUID correlationId);
}
