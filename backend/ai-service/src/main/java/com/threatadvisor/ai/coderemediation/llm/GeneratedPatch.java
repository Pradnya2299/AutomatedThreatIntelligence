package com.threatadvisor.ai.coderemediation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedPatch {

    @NotNull
    private List<FilePatch> files = new ArrayList<>();
    @NotBlank
    private String summary;
    @NotBlank
    @Pattern(regexp = "HIGH|MEDIUM|LOW")
    private String confidence;

    public List<FilePatch> getFiles() { return files; }
    public void setFiles(List<FilePatch> files) { this.files = files == null ? new ArrayList<>() : files; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilePatch {
        @NotBlank
        private String path;
        @NotBlank
        @Pattern(regexp = "MODIFY|ADD|DELETE")
        private String operation;
        @NotBlank
        private String originalContentHash;
        @NotBlank
        private String proposedContent;
        private String explanation;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getOriginalContentHash() { return originalContentHash; }
        public void setOriginalContentHash(String originalContentHash) { this.originalContentHash = originalContentHash; }
        public String getProposedContent() { return proposedContent; }
        public void setProposedContent(String proposedContent) { this.proposedContent = proposedContent; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
