package com.threatadvisor.ai.coderemediation.llm;

import com.threatadvisor.ai.coderemediation.CodeContextSelector;
import com.threatadvisor.ai.coderemediation.patch.DockerfilePatcher;
import com.threatadvisor.ai.coderemediation.patch.MavenDependencyPatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic structured stand-in used when AI_DEMO_MODE=true. Not an OpenAI response.
 */
@Service
@ConditionalOnProperty(name = "ai.demo-mode", havingValue = "true", matchIfMissing = true)
public class DemoCodeRemediationLlm implements CodeRemediationLlm {

    private static final Pattern FILE = Pattern.compile("--- FILE: (.+)\\n([\\s\\S]*?)--- END FILE: \\1\\n");

    @Override
    public CodeAnalysisResponse analyze(String userPrompt) {
        Files files = Files.parse(userPrompt);
        CodeAnalysisResponse response = new CodeAnalysisResponse();
        if (files.pom != null && files.pom.contains("log4j-core")) {
            fill(response, "Dependency Vulnerability", "log4j-core", "Vulnerable Log4j core version in pom.xml",
                    "pom.xml", "DEPENDENCY_UPGRADE", "HIGH");
        } else if (files.docker != null && files.docker.toLowerCase(Locale.ROOT).contains("openjdk:8")) {
            fill(response, "Container Base Image", "base-image", "Outdated JDK 8 container base image",
                    "Dockerfile", "BASE_IMAGE_UPGRADE", "HIGH");
        } else if (files.java != null && files.java.contains("MD5")) {
            fill(response, "Insecure Cryptography", "MessageDigest", "MD5 is a broken hash algorithm",
                    "src/main/java/com/northwind/InsecureHash.java", "SOURCE_CODE_CHANGE", "HIGH");
        } else {
            fill(response, "Unknown", "unknown", "No reliable repository evidence",
                    null, "REVIEW_REQUIRED", "LOW");
        }
        return response;
    }

    @Override
    public LlmPatchPlan plan(String userPrompt) {
        CodeAnalysisResponse analysis = analyze(userPrompt);
        LlmPatchPlan plan = new LlmPatchPlan();
        plan.setSummary("[DEMO MODE] Planned change; not from OpenAI. " + analysis.getRootCause());
        plan.setConfidence(analysis.getConfidence());
        plan.setRollbackPlan("Revert the isolated ai-security branch.");
        plan.setValidationCommands(List.of("mvn -q test"));
        plan.setExpectedDiffSummary(analysis.getRemediationStrategy());
        if (!"REVIEW_REQUIRED".equals(analysis.getRemediationStrategy())
                && !"INSUFFICIENT_EVIDENCE".equals(analysis.getRemediationStrategy())
                && !analysis.getRelevantFiles().isEmpty()) {
            LlmPatchPlan.Change change = new LlmPatchPlan.Change();
            change.setFile(analysis.getRelevantFiles().getFirst().getPath());
            change.setOperation("MODIFY");
            change.setReason(analysis.getRootCause());
            change.setInstructions(List.of(analysis.getRecommendedChanges().isEmpty()
                    ? analysis.getRootCause()
                    : analysis.getRecommendedChanges().getFirst().getChange()));
            plan.setChanges(List.of(change));
        }
        return plan;
    }

    @Override
    public GeneratedPatch generate(String userPrompt) {
        Files files = Files.parse(userPrompt);
        GeneratedPatch patch = new GeneratedPatch();
        patch.setConfidence("HIGH");
        List<GeneratedPatch.FilePatch> out = new ArrayList<>();
        if (files.pom != null && MavenDependencyPatcher.containsArtifact(files.pom, "log4j-core")) {
            String after = MavenDependencyPatcher.upgrade(files.pom, "log4j-core", "2.17.1");
            out.add(file("pom.xml", files.pom, after, "Upgrade log4j-core to 2.17.1"));
            patch.setSummary("[DEMO MODE] Maven Log4j upgrade; not from OpenAI.");
        } else if (files.docker != null && DockerfilePatcher.currentFrom(files.docker) != null) {
            String after = DockerfilePatcher.replaceFrom(files.docker, "eclipse-temurin:17-jre");
            out.add(file("Dockerfile", files.docker, after, "Replace JDK 8 base image"));
            patch.setSummary("[DEMO MODE] Docker base image upgrade; not from OpenAI.");
        } else if (files.java != null && files.java.contains("MD5")) {
            String after = files.java.replace("MD5", "SHA-256");
            out.add(file("src/main/java/com/northwind/InsecureHash.java", files.java, after, "Replace MD5 with SHA-256"));
            patch.setSummary("[DEMO MODE] Insecure hash replacement; not from OpenAI.");
        } else {
            patch.setSummary("[DEMO MODE] No safe patch.");
            patch.setConfidence("LOW");
        }
        patch.setFiles(out);
        return patch;
    }

    @Override
    public String mode() {
        return "DEMO_MODE";
    }

    @Override
    public String modelName() {
        return "demo-deterministic";
    }

    private static GeneratedPatch.FilePatch file(String path, String before, String after, String explanation) {
        GeneratedPatch.FilePatch patch = new GeneratedPatch.FilePatch();
        patch.setPath(path);
        patch.setOperation("MODIFY");
        patch.setOriginalContentHash(ContentHashes.sha256(before));
        patch.setProposedContent(after);
        patch.setExplanation(explanation);
        return patch;
    }

    private static void fill(
            CodeAnalysisResponse response,
            String type,
            String component,
            String root,
            String file,
            String strategy,
            String confidence) {
        response.setVulnerabilityType(type);
        response.setAffectedComponent(component);
        response.setRootCause(root);
        response.setConfidence(confidence);
        response.setRemediationStrategy(strategy);
        response.setValidationPlan(List.of("Deterministic file verification after patch"));
        response.setRisks(List.of("Transitive dependencies may remain"));
        if (file != null) {
            CodeAnalysisResponse.RelevantFile relevant = new CodeAnalysisResponse.RelevantFile();
            relevant.setPath(file);
            relevant.setReason(root);
            response.setRelevantFiles(List.of(relevant));
            CodeAnalysisResponse.RecommendedChange change = new CodeAnalysisResponse.RecommendedChange();
            change.setFile(file);
            change.setChange(root);
            change.setReason(root);
            response.setRecommendedChanges(List.of(change));
        }
    }

    private record Files(String pom, String docker, String java) {
        static Files parse(String prompt) {
            String pom = null;
            String docker = null;
            String java = null;
            if (prompt == null) {
                return new Files(null, null, null);
            }
            Matcher matcher = FILE.matcher(prompt);
            while (matcher.find()) {
                String path = matcher.group(1).trim();
                String content = matcher.group(2);
                if ("pom.xml".equals(path)) {
                    pom = content;
                } else if ("Dockerfile".equals(path)) {
                    docker = content;
                } else if (path.endsWith("InsecureHash.java")) {
                    java = content;
                }
            }
            return new Files(pom, docker, java);
        }
    }
}
