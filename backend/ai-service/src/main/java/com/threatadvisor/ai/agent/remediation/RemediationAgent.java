package com.threatadvisor.ai.agent.remediation;

import com.threatadvisor.ai.agent.common.AgentToolException;
import com.threatadvisor.ai.agent.common.Confidence;
import com.threatadvisor.ai.agent.common.EvidenceItem;
import com.threatadvisor.ai.agent.common.EvidenceSource;
import com.threatadvisor.ai.agent.common.SecurityAgent;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.agent.dto.RemediationAgentResult;
import com.threatadvisor.ai.agent.dto.RemediationToolResult;
import com.threatadvisor.ai.agent.orchestrator.InvestigationPlanner;
import com.threatadvisor.ai.agent.tool.KnowledgeSearchTool;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
import com.threatadvisor.ai.agent.tool.RemediationGenerationTool;
import org.springframework.stereotype.Component;

@Component
public class RemediationAgent implements SecurityAgent {

    public static final String NAME = "RemediationAgent";

    private final RemediationGenerationTool generationTool;
    private final KnowledgeSearchTool knowledgeSearchTool;
    private final PolicyRetrievalTool policyRetrievalTool;

    public RemediationAgent(
            RemediationGenerationTool generationTool,
            KnowledgeSearchTool knowledgeSearchTool,
            PolicyRetrievalTool policyRetrievalTool) {
        this.generationTool = generationTool;
        this.knowledgeSearchTool = knowledgeSearchTool;
        this.policyRetrievalTool = policyRetrievalTool;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SecurityInvestigationContext execute(SecurityInvestigationContext context) {
        if (!InvestigationPlanner.remediationGate(context)) {
            throw new AgentToolException("REMEDIATION_GATE", "Remediation requires threat, assets, and deterministic risk");
        }
        recordKnowledge(context.cveId());
        RemediationToolResult generated = generationTool.generate(
                context.risk().primaryFindingId(), context.correlationId());
        Confidence confidence = generated.ragContextUsed() ? Confidence.HIGH : Confidence.MEDIUM;
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
                generated.summary(),
                confidence);
        return context.withRemediation(result)
                .withEvidenceItem(EvidenceItem.fact(
                        EvidenceSource.KNOWLEDGE_BASE,
                        "rag",
                        "Retrieved knowledge used by Phase 3 generator",
                        String.valueOf(generated.ragSources()),
                        confidence));
    }

    private void recordKnowledge(String cveId) {
        try {
            KnowledgeSearchResult knowledge = knowledgeSearchTool.search(cveId);
            policyRetrievalTool.retrievePolicies(cveId);
            knowledge.hits();
        } catch (RuntimeException ignored) {
            // RAG evidence is optional; generation still uses Phase 3 internals
        }
    }
}
