package com.threatadvisor.ai.agent.risk;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.SecurityAgent;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.FindingRiskScore;
import com.threatadvisor.ai.agent.dto.RiskAnalystResult;
import com.threatadvisor.ai.agent.dto.RiskEngineSnapshot;
import com.threatadvisor.ai.agent.tool.RiskCalculationTool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RiskAnalystAgent implements SecurityAgent {

    public static final String NAME = "RiskAnalystAgent";

    private final RiskCalculationTool riskCalculationTool;

    public RiskAnalystAgent(RiskCalculationTool riskCalculationTool) {
        this.riskCalculationTool = riskCalculationTool;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SecurityInvestigationContext execute(SecurityInvestigationContext context) {
        if (context.assets() == null) {
            throw new AgentToolException("ASSETS_REQUIRED", "Risk analysis requires asset investigation first");
        }
        if (!context.assets().affected() || context.assets().assets().isEmpty()) {
            throw new AgentToolException("NOT_EXPOSED", "Risk engine is not invoked when no assets are affected");
        }
        List<FindingRiskScore> scores = new ArrayList<>();
        for (AffectedAssetMatch match : context.assets().assets()) {
            RiskEngineSnapshot snapshot = riskCalculationTool.calculate(match.findingId(), context.correlationId());
            scores.add(new FindingRiskScore(
                    match.findingId(),
                    match.assetId(),
                    snapshot.riskAssessmentId(),
                    snapshot.riskScore(),
                    snapshot.riskLevel(),
                    snapshot.factors(),
                    snapshot.explanation()));
        }
        FindingRiskScore primary = scores.stream()
                .max(Comparator.comparing(s -> s.riskScore() == null ? BigDecimal.ZERO : s.riskScore()))
                .orElseThrow(() -> new AgentToolException("RISK_EMPTY", "Risk engine returned no scores"));
        String explanation = "Organizational urgency is " + primary.riskLevel()
                + " (" + primary.riskScore() + ") from the deterministic risk engine for finding "
                + primary.findingId() + ". " + (primary.calculationDetails() == null ? "" : primary.calculationDetails());
        RiskAnalystResult result = new RiskAnalystResult(
                primary.findingId(),
                primary.riskAssessmentId(),
                primary.riskScore(),
                primary.riskLevel(),
                primary.factors(),
                explanation,
                List.copyOf(scores),
                com.threatadvisor.ai.agent.common.Confidence.HIGH);
        return context.withRisk(result)
                .withEvidenceItem(com.threatadvisor.ai.agent.common.EvidenceItem.fact(
                        com.threatadvisor.ai.agent.common.EvidenceSource.RISK_ENGINE,
                        "risk_score",
                        "Deterministic risk engine v1",
                        primary.riskScore() + " " + primary.riskLevel(),
                        com.threatadvisor.ai.agent.common.Confidence.HIGH));
    }
}
