package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationLlm;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationPromptFactory;
import com.threatadvisor.ai.coderemediation.llm.GeneratedPatch;
import com.threatadvisor.ai.coderemediation.llm.LlmPatchPlan;
import com.threatadvisor.ai.coderemediation.llm.LlmUnavailableException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatchGenerationAgent {

    public static final String NAME = "PatchGenerationAgent";

    private final CodeRemediationLlm llm;
    private final CodeRemediationPromptFactory prompts;
    private final PolicyRetrievalTool policies;

    public PatchGenerationAgent(
            CodeRemediationLlm llm, CodeRemediationPromptFactory prompts, PolicyRetrievalTool policies) {
        this.llm = llm;
        this.prompts = prompts;
        this.policies = policies;
    }

    public GeneratedPatch generate(
            SecurityInvestigationContext investigation,
            List<CodeContextSelector.WorkspaceFile> files,
            LlmPatchPlan plan,
            String previousFailure) {
        try {
            GeneratedPatch patch = llm.generate(prompts.generatePrompt(
                    investigation,
                    files,
                    policies.retrievePolicies(investigation.cveId() + " patch generation"),
                    plan,
                    previousFailure));
            if (patch == null || patch.getFiles() == null || patch.getConfidence() == null) {
                throw new LlmUnavailableException("Invalid structured generated patch");
            }
            patch.setFiles(new java.util.ArrayList<>(patch.getFiles()));
            patch.getFiles().removeIf(file -> !CodeContextSelector.allowed(file.getPath(), files));
            return patch;
        } catch (LlmUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException("Patch generation failed", ex);
        }
    }
}
