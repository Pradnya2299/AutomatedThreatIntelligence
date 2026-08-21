package com.threatadvisor.ai.agent.threat;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.EvidenceItem;
import com.threatadvisor.ai.agent.common.EvidenceSource;
import com.threatadvisor.ai.agent.common.InvestigationConfidence;
import com.threatadvisor.ai.agent.common.SecurityAgent;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedProduct;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.agent.tool.CpeLookupTool;
import com.threatadvisor.ai.agent.tool.CveLookupTool;
import com.threatadvisor.ai.agent.tool.VulnerabilityContextTool;
import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.domain.VulnerabilityCpe;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ThreatIntelligenceAgent implements SecurityAgent {

    public static final String NAME = "ThreatIntelligenceAgent";

    private final CveLookupTool cveLookupTool;
    private final CpeLookupTool cpeLookupTool;
    private final VulnerabilityContextTool contextTool;

    public ThreatIntelligenceAgent(
            CveLookupTool cveLookupTool, CpeLookupTool cpeLookupTool, VulnerabilityContextTool contextTool) {
        this.cveLookupTool = cveLookupTool;
        this.cpeLookupTool = cpeLookupTool;
        this.contextTool = contextTool;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SecurityInvestigationContext execute(SecurityInvestigationContext context) {
        Vulnerability vulnerability = cveLookupTool.findByCveId(context.cveId())
                .orElseThrow(() -> new AgentToolException(
                        "CVE_NOT_FOUND",
                        "No vulnerability row for " + context.cveId()));
        List<VulnerabilityCpe> cpes = cpeLookupTool.lookupCpes(vulnerability.getId());
        List<AffectedProduct> products = new ArrayList<>();
        if (vulnerability.getAffectedProducts() != null) {
            String[] vendors = vulnerability.getAffectedVendors() == null
                    ? new String[0]
                    : vulnerability.getAffectedVendors();
            String[] names = vulnerability.getAffectedProducts();
            for (int i = 0; i < names.length; i++) {
                products.add(new AffectedProduct(
                        i < vendors.length ? vendors[i] : null,
                        names[i],
                        null,
                        null,
                        null));
            }
        }
        for (VulnerabilityCpe cpe : cpes) {
            products.add(new AffectedProduct(
                    cpe.getVendor(),
                    cpe.getProduct(),
                    cpe.getVersionStartIncluding(),
                    cpe.getVersionEndExcluding(),
                    null));
        }
        String exploitability = exploitability(vulnerability);
        List<EvidenceItem> evidence = List.of(
                EvidenceItem.fact(EvidenceSource.CVE_DATABASE, "cve_row", "Canonical CVE row", vulnerability.getCveId(),
                        Confidence.HIGH),
                EvidenceItem.fact(EvidenceSource.CPE_LOOKUP, "cpe_count", "CPE rows from vulnerability_cpe",
                        String.valueOf(cpes.size()), cpes.isEmpty() ? Confidence.MEDIUM : Confidence.HIGH),
                EvidenceItem.fact(EvidenceSource.CVE_DATABASE, "intelligence_source",
                        "Vulnerability intelligence source",
                        vulnerability.getIntelligenceSource() == null
                                ? (vulnerability.getSource() == null ? "SEED" : vulnerability.getSource())
                                : vulnerability.getIntelligenceSource(),
                        Confidence.HIGH));
        ThreatIntelligenceResult incomplete = new ThreatIntelligenceResult(
                vulnerability.getCveId(),
                vulnerability.getId(),
                vulnerability.getSeverity(),
                vulnerability.getCvssScore(),
                exploitability,
                vulnerability.getExploitAvailable(),
                vulnerability.getActivelyExploited(),
                List.copyOf(products),
                contextTool.summarize(vulnerability),
                evidence,
                null);
        ThreatIntelligenceResult result = new ThreatIntelligenceResult(
                incomplete.cveId(),
                incomplete.vulnerabilityId(),
                incomplete.severity(),
                incomplete.cvssScore(),
                incomplete.exploitability(),
                incomplete.exploitAvailable(),
                incomplete.activelyExploited(),
                incomplete.affectedProducts(),
                incomplete.summary(),
                evidence,
                InvestigationConfidence.threat(vulnerability, incomplete));
        return context.withVulnerability(vulnerability).withThreat(result);
    }

    private static String exploitability(Vulnerability vulnerability) {
        if (Boolean.TRUE.equals(vulnerability.getActivelyExploited())) {
            return "SOURCE_FLAGS_ACTIVE_EXPLOITATION";
        }
        if (Boolean.TRUE.equals(vulnerability.getExploitAvailable())) {
            return "SOURCE_FLAGS_EXPLOIT_AVAILABLE";
        }
        if (vulnerability.getExploitAvailable() == null && vulnerability.getActivelyExploited() == null) {
            return "UNKNOWN_FROM_SOURCE";
        }
        return "NO_EXPLOIT_FLAG_IN_SOURCE";
    }
}
