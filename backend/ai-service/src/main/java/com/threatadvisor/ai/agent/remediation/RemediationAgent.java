package com.threatadvisor.ai.agent.remediation;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.SecurityAgent;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.RemediationAgentResult;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.agent.tool.RemediationGenerationTool;
import org.springframework.stereotype.Component;

@Component
public class RemediationAgent implements SecurityAgent {

    public static final String NAME = "RemediationAgent";

    private final RemediationGenerationTool generationTool;

    public RemediationAgent(RemediationGenerationTool generationTool) {
        this.generationTool = generationTool;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SecurityInvestigationContext execute(SecurityInvestigationContext context) {
        if (context.risk() == null || context.risk().primaryFindingId() == null) {
            throw new AgentToolException("RISK_REQUIRED", "Remediation requires a deterministic risk assessment");
        }
        RemediationToolResult generated = generationTool.generate(
                context.risk().primaryFindingId(), context.correlationId());
        RemediationAgentResult result = new RemediationAgentResult(
                generated.findingId(),
                generated.riskAssessmentId(),
                generated.remediationPlanId(),
                generated.status(),
                generated.priority(),
                generated.targetVersion(),
                generated.prerequisites(),
                generated.implementationSteps(),
                generated.validationSteps(),
                generated.rollback(),
                generated.references(),
                generated.ragSources(),
                generated.ragContextUsed(),
                generated.summary());
        return context.withRemediation(result);
    }
}
