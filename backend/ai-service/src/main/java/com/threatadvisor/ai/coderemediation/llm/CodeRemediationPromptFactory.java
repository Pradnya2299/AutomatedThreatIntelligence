package com.threatadvisor.ai.coderemediation.llm;

import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import com.threatadvisor.ai.coderemediation.CodeContextSelector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CodeRemediationPromptFactory {

    public static final String PROMPT_VERSION = "code-remediation-v1";

    public String analysisPrompt(
            SecurityInvestigationContext investigation,
            List<CodeContextSelector.WorkspaceFile> files,
            KnowledgeSearchResult rag) {
        return base(investigation, files, rag) + "\nTask: produce CodeAnalysisResponse JSON fields only.\n";
    }

    public String planPrompt(
            SecurityInvestigationContext investigation,
            List<CodeContextSelector.WorkspaceFile> files,
            KnowledgeSearchResult rag,
            CodeAnalysisResponse analysis) {
        return base(investigation, files, rag)
                + "\nCode analysis:\n"
                + analysis.getRemediationStrategy() + " " + analysis.getRootCause()
                + "\nTask: produce PatchPlan. Do not emit file bodies.\n";
    }

    public String generatePrompt(
            SecurityInvestigationContext investigation,
            List<CodeContextSelector.WorkspaceFile> files,
            KnowledgeSearchResult rag,
            LlmPatchPlan plan,
            String previousFailure) {
        StringBuilder hashes = new StringBuilder();
        for (CodeContextSelector.WorkspaceFile file : files) {
            hashes.append(file.path()).append('=').append(ContentHashes.sha256(file.content())).append('\n');
        }
        return base(investigation, files, rag)
                + "\nPlan summary: " + (plan == null ? "" : plan.getSummary())
                + "\nSHA-256 of original files:\n" + hashes
                + (previousFailure == null ? "" : "\nPrevious validation failure (deterministic): " + previousFailure)
                + "\nTask: produce GeneratedPatch with full proposedContent for each modified file.\n";
    }

    private static String base(
            SecurityInvestigationContext investigation,
            List<CodeContextSelector.WorkspaceFile> files,
            KnowledgeSearchResult rag) {
        StringBuilder out = new StringBuilder();
        out.append("CVE: ").append(investigation.cveId()).append('\n');
        if (investigation.threat() != null) {
            out.append("Severity: ").append(investigation.threat().severity()).append('\n');
            out.append("CVSS: ").append(investigation.threat().cvssScore()).append('\n');
            out.append("NVD summary: ").append(investigation.threat().summary()).append('\n');
        }
        if (investigation.risk() != null) {
            out.append("Risk score (deterministic engine): ").append(investigation.risk().riskScore()).append('\n');
        }
        if (investigation.remediation() != null && investigation.remediation().targetVersion() != null) {
            out.append("Fixed/target version from remediation plan: ")
                    .append(investigation.remediation().targetVersion()).append('\n');
        }
        out.append("RAG policies:\n");
        if (rag != null && rag.hits() != null) {
            out.append(rag.hits().stream()
                    .map(KnowledgeChunkHit::content)
                    .limit(5)
                    .collect(Collectors.joining("\n---\n")));
        }
        out.append("\nRepository files (only these exist):\n");
        out.append(CodeContextSelector.formatForPrompt(files));
        return out.toString();
    }
}
