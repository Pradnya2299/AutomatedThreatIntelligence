package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
import com.threatadvisor.ai.coderemediation.llm.CodeAnalysisResponse;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationLlm;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationPromptFactory;
import com.threatadvisor.ai.coderemediation.llm.LlmPatchPlan;
import com.threatadvisor.ai.coderemediation.llm.LlmUnavailableException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatchPlanningAgent {

    public static final String NAME = "PatchPlanningAgent";

    private final CodeRemediationLlm llm;
    private final CodeRemediationPromptFactory prompts;
    private final PolicyRetrievalTool policies;

    public PatchPlanningAgent(CodeRemediationLlm llm, CodeRemediationPromptFactory prompts, PolicyRetrievalTool policies) {
        this.llm = llm;
        this.prompts = prompts;
        this.policies = policies;
    }

    public LlmPatchPlan plan(
            SecurityInvestigationContext investigation,
            List<CodeContextSelector.WorkspaceFile> files,
            CodeAnalysisResponse analysis) {
        try {
            LlmPatchPlan plan = llm.plan(prompts.planPrompt(
                    investigation, files, policies.retrievePolicies(investigation.cveId() + " change management"), analysis));
            if (plan == null || plan.getSummary() == null || plan.getConfidence() == null) {
                throw new LlmUnavailableException("Invalid structured patch plan");
            }
            plan.setChanges(new java.util.ArrayList<>(plan.getChanges()));
            plan.getChanges().removeIf(change -> !CodeContextSelector.allowed(change.getFile(), files));
            return plan;
        } catch (LlmUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException("Patch planning failed", ex);
        }
    }
}
