package com.threatadvisor.ai.agent.threat;

import com.threatadvisor.ai.agent.common.AgentStatus;
import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.agent.tool.CpeLookupTool;
import com.threatadvisor.ai.agent.tool.CveLookupTool;
import com.threatadvisor.ai.agent.tool.VulnerabilityContextTool;
import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.domain.VulnerabilityCpe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreatIntelligenceAgentTest {

    @Mock
    private CveLookupTool cveLookupTool;
    @Mock
    private CpeLookupTool cpeLookupTool;
    @Mock
    private VulnerabilityContextTool contextTool;

    private ThreatIntelligenceAgent agent;

    @BeforeEach
    void setUp() {
        agent = new ThreatIntelligenceAgent(cveLookupTool, cpeLookupTool, contextTool);
    }

    @Test
    void usesCveLookupAndDoesNotInventExploitation() {
        Vulnerability vulnerability = vulnerability("CVE-2021-44228");
        when(cveLookupTool.findByCveId("CVE-2021-44228")).thenReturn(Optional.of(vulnerability));
        when(cpeLookupTool.lookupCpes(vulnerability.getId())).thenReturn(List.of(cpe(vulnerability.getId())));
        when(contextTool.summarize(vulnerability)).thenReturn("deterministic summary");

        SecurityInvestigationContext result = agent.execute(baseContext());

        ThreatIntelligenceResult threat = result.threat();
        assertEquals("CVE-2021-44228", threat.cveId());
        assertEquals(new BigDecimal("10.0"), threat.cvssScore());
        assertEquals("UNKNOWN_FROM_SOURCE", threat.exploitability());
        assertEquals("log4j", threat.affectedProducts().getFirst().product());
        assertEquals("deterministic summary", threat.summary());
        verify(cveLookupTool).findByCveId("CVE-2021-44228");
        verify(cpeLookupTool).lookupCpes(vulnerability.getId());
    }

    @Test
    void missingCveFailsWithoutGuessing() {
        when(cveLookupTool.findByCveId("CVE-1999-0001")).thenReturn(Optional.empty());
        AgentToolException ex = assertThrows(AgentToolException.class, () -> agent.execute(
                baseContext().toBuilder().cveId("CVE-1999-0001").build()));
        assertEquals("CVE_NOT_FOUND", ex.getCode());
        verify(contextTool, never()).summarize(any());
    }

    @Test
    void observedFailureMarksAgentFailed() {
        when(cveLookupTool.findByCveId(any())).thenReturn(Optional.empty());
        SecurityInvestigationContext failed = agent.runObserved(baseContext(), ctx -> ctx);
        assertEquals(AgentStatus.FAILED, failed.executionOf(ThreatIntelligenceAgent.NAME).orElseThrow().status());
        assertTrue(failed.errors().stream().anyMatch(e -> "CVE_NOT_FOUND".equals(e.code())));
        assertEquals(InvestigationStatus.RUNNING, failed.status());
    }

    private static SecurityInvestigationContext baseContext() {
        return SecurityInvestigationContext.builder()
                .investigationId(UUID.randomUUID())
                .cveId("CVE-2021-44228")
                .correlationId(UUID.randomUUID())
                .status(InvestigationStatus.RUNNING)
                .build();
    }

    private static Vulnerability vulnerability(String cveId) {
        Vulnerability v = new Vulnerability();
        v.setId(UUID.randomUUID());
        v.setCveId(cveId);
        v.setSeverity("CRITICAL");
        v.setCvssScore(new BigDecimal("10.0"));
        v.setDescription("Remote code execution in Log4j");
        v.setAffectedProducts(new String[] {"log4j"});
        v.setAffectedVendors(new String[] {"apache"});
        return v;
    }

    private static VulnerabilityCpe cpe(UUID vulnerabilityId) {
        VulnerabilityCpe cpe = new VulnerabilityCpe();
        cpe.setId(UUID.randomUUID());
        cpe.setVulnerabilityId(vulnerabilityId);
        cpe.setVendor("apache");
        cpe.setProduct("log4j");
        cpe.setVersionStartIncluding("2.0.0");
        cpe.setVersionEndExcluding("2.17.0");
        return cpe;
    }
}
