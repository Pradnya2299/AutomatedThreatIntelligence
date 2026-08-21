package com.threatadvisor.ai.coderemediation.llm;

import com.threatadvisor.ai.coderemediation.CodeContextSelector;
import com.threatadvisor.ai.coderemediation.patch.MavenDependencyPatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoCodeRemediationLlmTest {

    private final DemoCodeRemediationLlm llm = new DemoCodeRemediationLlm();

    @Test
    void analyzesMavenLog4j() {
        String pom = """
                <project><dependencies><dependency>
                <groupId>org.apache.logging.log4j</groupId>
                <artifactId>log4j-core</artifactId>
                <version>2.14.1</version>
                </dependency></dependencies></project>
                """;
        String prompt = CodeContextSelector.formatForPrompt(List.of(new CodeContextSelector.WorkspaceFile("pom.xml", pom)));
        CodeAnalysisResponse analysis = llm.analyze(prompt);
        assertEquals("DEPENDENCY_UPGRADE", analysis.getRemediationStrategy());
        assertEquals("HIGH", analysis.getConfidence());
        GeneratedPatch patch = llm.generate(prompt);
        assertTrue(MavenDependencyPatcher.containsArtifact(patch.getFiles().getFirst().getProposedContent(), "log4j-core"));
        assertTrue(patch.getFiles().getFirst().getProposedContent().contains("2.17.1"));
        assertEquals("DEMO_MODE", llm.mode());
    }

    @Test
    void insufficientEvidenceIsReview() {
        String prompt = CodeContextSelector.formatForPrompt(List.of(
                new CodeContextSelector.WorkspaceFile("README.md", "nothing here")));
        CodeAnalysisResponse analysis = llm.analyze(prompt);
        assertEquals("REVIEW_REQUIRED", analysis.getRemediationStrategy());
        assertEquals("LOW", analysis.getConfidence());
    }
}
