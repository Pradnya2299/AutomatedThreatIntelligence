package com.threatadvisor.ai.coderemediation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeAnalysisResponse {

    @NotBlank
    private String vulnerabilityType;
    @NotBlank
    private String affectedComponent;
    @NotBlank
    private String rootCause;
    @NotBlank
    @Pattern(regexp = "HIGH|MEDIUM|LOW")
    private String confidence;
    @NotNull
    private List<RelevantFile> relevantFiles = new ArrayList<>();
    @NotBlank
    @Pattern(regexp = "DEPENDENCY_UPGRADE|SOURCE_CODE_CHANGE|CONFIGURATION_CHANGE|BASE_IMAGE_UPGRADE|REVIEW_REQUIRED|INSUFFICIENT_EVIDENCE")
    private String remediationStrategy;
    @NotNull
    private List<RecommendedChange> recommendedChanges = new ArrayList<>();
    @NotNull
    private List<String> validationPlan = new ArrayList<>();
    @NotNull
    private List<String> risks = new ArrayList<>();

    public String getVulnerabilityType() { return vulnerabilityType; }
    public void setVulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; }
    public String getAffectedComponent() { return affectedComponent; }
    public void setAffectedComponent(String affectedComponent) { this.affectedComponent = affectedComponent; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public List<RelevantFile> getRelevantFiles() { return relevantFiles; }
    public void setRelevantFiles(List<RelevantFile> relevantFiles) {
        this.relevantFiles = relevantFiles == null ? new ArrayList<>() : relevantFiles;
    }
    public String getRemediationStrategy() { return remediationStrategy; }
    public void setRemediationStrategy(String remediationStrategy) { this.remediationStrategy = remediationStrategy; }
    public List<RecommendedChange> getRecommendedChanges() { return recommendedChanges; }
    public void setRecommendedChanges(List<RecommendedChange> recommendedChanges) {
        this.recommendedChanges = recommendedChanges == null ? new ArrayList<>() : recommendedChanges;
    }
    public List<String> getValidationPlan() { return validationPlan; }
    public void setValidationPlan(List<String> validationPlan) {
        this.validationPlan = validationPlan == null ? new ArrayList<>() : validationPlan;
    }
    public List<String> getRisks() { return risks; }
    public void setRisks(List<String> risks) { this.risks = risks == null ? new ArrayList<>() : risks; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelevantFile {
        @NotBlank
        private String path;
        private String reason;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecommendedChange {
        @NotBlank
        private String file;
        private String change;
        private String reason;
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        public String getChange() { return change; }
        public void setChange(String change) { this.change = change; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
