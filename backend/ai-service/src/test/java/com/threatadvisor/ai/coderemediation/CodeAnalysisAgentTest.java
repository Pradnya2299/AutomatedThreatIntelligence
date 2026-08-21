package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.agent.tool.PolicyRetrievalTool;
import com.threatadvisor.ai.coderemediation.llm.CodeAnalysisResponse;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationLlm;
import com.threatadvisor.ai.coderemediation.llm.CodeRemediationPromptFactory;
import com.threatadvisor.ai.coderemediation.llm.GeneratedPatch;
import com.threatadvisor.ai.coderemediation.llm.LlmPatchPlan;
import com.threatadvisor.ai.coderemediation.llm.LlmUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeAnalysisAgentTest {

    @Test
    void dropsHallucinatedPaths() {
        CodeRemediationLlm llm = new StubLlm();
        PolicyRetrievalTool policies = query -> new KnowledgeSearchResult(List.of());
        CodeAnalysisAgent agent = new CodeAnalysisAgent(llm, new CodeRemediationPromptFactory(), policies);
        SecurityInvestigationContext ctx = SecurityInvestigationContext.builder().cveId("CVE-2021-44228").build();
        List<CodeContextSelector.WorkspaceFile> files = List.of(
                new CodeContextSelector.WorkspaceFile("pom.xml", "<project/>"));
        CodeAnalysisResponse response = agent.analyze(ctx, files);
        assertEquals(1, response.getRelevantFiles().size());
        assertEquals("pom.xml", response.getRelevantFiles().getFirst().getPath());
        assertTrue(response.getRecommendedChanges().stream().noneMatch(c -> c.getFile().contains("etc/passwd")));
    }

    @Test
    void invalidStructuredResponseIsUnavailable() {
        CodeRemediationLlm llm = new CodeRemediationLlm() {
            @Override public CodeAnalysisResponse analyze(String userPrompt) { return new CodeAnalysisResponse(); }
            @Override public LlmPatchPlan plan(String userPrompt) { return null; }
            @Override public GeneratedPatch generate(String userPrompt) { return null; }
            @Override public String mode() { return "LLM_POWERED"; }
            @Override public String modelName() { return "test"; }
        };
        CodeAnalysisAgent agent = new CodeAnalysisAgent(llm, new CodeRemediationPromptFactory(), q -> new KnowledgeSearchResult(List.of()));
        assertThrows(LlmUnavailableException.class, () -> agent.analyze(
                SecurityInvestigationContext.builder().cveId("CVE-1").build(),
                List.of(new CodeContextSelector.WorkspaceFile("pom.xml", "<x/>"))));
    }

    private static final class StubLlm implements CodeRemediationLlm {
        @Override
        public CodeAnalysisResponse analyze(String userPrompt) {
            CodeAnalysisResponse response = new CodeAnalysisResponse();
            response.setVulnerabilityType("dep");
            response.setAffectedComponent("log4j-core");
            response.setRootCause("old version");
            response.setConfidence("HIGH");
            response.setRemediationStrategy("DEPENDENCY_UPGRADE");
            CodeAnalysisResponse.RelevantFile real = new CodeAnalysisResponse.RelevantFile();
            real.setPath("pom.xml");
            CodeAnalysisResponse.RelevantFile fake = new CodeAnalysisResponse.RelevantFile();
            fake.setPath("/etc/passwd");
            response.setRelevantFiles(List.of(real, fake));
            CodeAnalysisResponse.RecommendedChange change = new CodeAnalysisResponse.RecommendedChange();
            change.setFile("/etc/passwd");
            change.setChange("no");
            response.setRecommendedChanges(List.of(change));
            return response;
        }
        @Override public LlmPatchPlan plan(String userPrompt) { return new LlmPatchPlan(); }
        @Override public GeneratedPatch generate(String userPrompt) { return new GeneratedPatch(); }
        @Override public String mode() { return "LLM_POWERED"; }
        @Override public String modelName() { return "stub"; }
    }
}
