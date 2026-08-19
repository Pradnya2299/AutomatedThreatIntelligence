package com.threatadvisor.ai.dto;

import com.threatadvisor.ai.agent.common.AgentError;
import com.threatadvisor.ai.agent.common.AgentExecution;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.DecisionRecord;
import com.threatadvisor.ai.agent.common.EvidenceItem;
import com.threatadvisor.ai.agent.common.ExecutionTraceEntry;
import com.threatadvisor.ai.agent.common.InvestigationState;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.FinalRecommendation;
import com.threatadvisor.ai.agent.dto.RemediationAgentResult;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvestigationResponse(
        UUID investigationId,
        String cveId,
        InvestigationStatus status,
        UUID correlationId,
        Instant startedAt,
        Instant completedAt,
        ThreatIntelligenceResult threatIntelligence,
        AssetInvestigationResult assetInvestigation,
        RiskAnalystResult riskAnalysis,
        RemediationAgentResult remediation,
        FinalRecommendation recommendation,
        List<AgentExecution> executions,
        List<AgentError> errors,
        List<EvidenceItem> evidence,
        InvestigationState currentState,
        Confidence confidence,
        List<DecisionRecord> decisionHistory,
        List<ExecutionTraceEntry> executionTrace
) {
    public static InvestigationResponse from(SecurityInvestigationContext context) {
        return new InvestigationResponse(
                context.investigationId(),
                context.cveId(),
                context.status(),
                context.correlationId(),
                context.startedAt(),
                context.completedAt(),
                context.threat(),
                context.assets(),
                context.risk(),
                context.remediation(),
                context.recommendation(),
                context.executions(),
                context.errors(),
                context.evidence(),
                context.currentState(),
                context.overallConfidence(),
                context.decisionHistory(),
                context.executionTrace());
    }
}
