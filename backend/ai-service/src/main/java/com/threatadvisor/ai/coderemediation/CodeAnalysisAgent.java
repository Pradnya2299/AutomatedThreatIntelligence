package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
import com.threatadvisor.ai.coderemediation.llm.CodeAnalysisResponse;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationLlm;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationPromptFactory;
import com.threatadvisor.ai.coderemediation.llm.LlmUnavailableException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodeAnalysisAgent {

    public static final String NAME = "CodeAnalysisAgent";

    private final CodeRemediationLlm llm;
    private final CodeRemediationPromptFactory prompts;
    private final PolicyRetrievalTool policies;

    public CodeAnalysisAgent(CodeRemediationLlm llm, CodeRemediationPromptFactory prompts, PolicyRetrievalTool policies) {
        this.llm = llm;
        this.prompts = prompts;
        this.policies = policies;
    }

    public CodeAnalysisResponse analyze(
            SecurityInvestigationContext investigation, List<CodeContextSelector.WorkspaceFile> files) {
        KnowledgeSearchResult rag = policies.retrievePolicies(
                investigation.cveId() + " secure coding dependency upgrade emergency patch rollback validation");
        try {
            CodeAnalysisResponse response = llm.analyze(prompts.analysisPrompt(investigation, files, rag));
            if (response == null || response.getRemediationStrategy() == null || response.getConfidence() == null) {
                throw new LlmUnavailableException("Invalid structured code analysis");
            }
            response.setRelevantFiles(new java.util.ArrayList<>(response.getRelevantFiles()));
            response.setRecommendedChanges(new java.util.ArrayList<>(response.getRecommendedChanges()));
            response.getRelevantFiles().removeIf(file -> !CodeContextSelector.allowed(file.getPath(), files));
            response.getRecommendedChanges().removeIf(change -> !CodeContextSelector.allowed(change.getFile(), files));
            return response;
        } catch (LlmUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException("Code analysis failed", ex);
        }
    }
}
