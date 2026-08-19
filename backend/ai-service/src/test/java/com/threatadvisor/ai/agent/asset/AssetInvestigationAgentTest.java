package com.threatadvisor.ai.agent.asset;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.CorrelationToolResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.agent.tool.AffectedAssetLookupTool;
import com.threatadvisor.ai.agent.tool.CorrelationTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetInvestigationAgentTest {

    @Mock
    private CorrelationTool correlationTool;
    @Mock
    private AffectedAssetLookupTool assetLookupTool;
    @InjectMocks
    private AssetInvestigationAgent agent;

    @Test
    void matchesComeFromCorrelationAndLookupTools() {
        UUID vulnerabilityId = UUID.randomUUID();
        UUID findingId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(correlationTool.correlate("CVE-2021-44228", correlationId))
                .thenReturn(new CorrelationToolResult("COMPLETED", "CVE-2021-44228", vulnerabilityId, UUID.randomUUID(), 1, 0, 4));
        when(assetLookupTool.lookup(vulnerabilityId)).thenReturn(List.of(new AffectedAssetMatch(
                findingId, assetId, "web-prod-01", "PROD", "CRITICAL", true,
                "EXACT_VERSION_MATCH", "HIGH", "deterministic match explanation")));

        SecurityInvestigationContext result = agent.execute(context(vulnerabilityId, correlationId));

        assertTrue(result.assets().affected());
        assertEquals(1, result.assets().affectedAssetCount());
        assertEquals("deterministic match explanation", result.assets().matchReasons().getFirst());
        verify(correlationTool).correlate("CVE-2021-44228", correlationId);
        verify(assetLookupTool).lookup(vulnerabilityId);
    }

    @Test
    void correlationFailureDoesNotClaimNotAffected() {
        UUID vulnerabilityId = UUID.randomUUID();
        when(correlationTool.correlate(any(), any()))
                .thenThrow(new AgentToolException("CORRELATION_UNAVAILABLE", "down"));

        AgentToolException ex = assertThrows(AgentToolException.class, () -> agent.execute(context(vulnerabilityId, UUID.randomUUID())));
        assertEquals("CORRELATION_UNAVAILABLE", ex.getCode());
    }

    @Test
    void zeroFindingsIsNotAffectedOnlyAfterSuccessfulCorrelation() {
        UUID vulnerabilityId = UUID.randomUUID();
        when(correlationTool.correlate(any(), any()))
                .thenReturn(new CorrelationToolResult("COMPLETED", "CVE-2021-44228", vulnerabilityId, UUID.randomUUID(), 0, 0, 12));
        when(assetLookupTool.lookup(vulnerabilityId)).thenReturn(List.of());

        SecurityInvestigationContext result = agent.execute(context(vulnerabilityId, UUID.randomUUID()));
        assertFalse(result.assets().affected());
        assertEquals(0, result.assets().affectedAssetCount());
        assertEquals(12, result.assets().matchesEvaluated());
    }

    private static SecurityInvestigationContext context(UUID vulnerabilityId, UUID correlationId) {
        return SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .correlationId(correlationId)
                .status(InvestigationStatus.RUNNING)
                .threat(new ThreatIntelligenceResult(
                        "CVE-2021-44228", vulnerabilityId, "CRITICAL", null, "UNKNOWN_FROM_SOURCE",
                        null, null, List.of(), "summary", List.of()))
                .build();
    }
}
