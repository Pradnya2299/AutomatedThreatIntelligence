package com.threatadvisor.ai.coderemediation.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmPatchPlan {

    @NotBlank
    private String summary;
    @NotNull
    private List<Change> changes = new ArrayList<>();
    private String expectedDiffSummary;
    @NotNull
    private List<String> validationCommands = new ArrayList<>();
    private String rollbackPlan;
    @NotBlank
    @Pattern(regexp = "HIGH|MEDIUM|LOW")
    private String confidence;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<Change> getChanges() { return changes; }
    public void setChanges(List<Change> changes) { this.changes = changes == null ? new ArrayList<>() : changes; }
    public String getExpectedDiffSummary() { return expectedDiffSummary; }
    public void setExpectedDiffSummary(String expectedDiffSummary) { this.expectedDiffSummary = expectedDiffSummary; }
    public List<String> getValidationCommands() { return validationCommands; }
    public void setValidationCommands(List<String> validationCommands) {
        this.validationCommands = validationCommands == null ? new ArrayList<>() : validationCommands;
    }
    public String getRollbackPlan() { return rollbackPlan; }
    public void setRollbackPlan(String rollbackPlan) { this.rollbackPlan = rollbackPlan; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        @NotBlank
        private String file;
        @NotBlank
        @Pattern(regexp = "MODIFY|ADD|DELETE")
        private String operation;
        private String reason;
        private List<String> instructions = new ArrayList<>();
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public List<String> getInstructions() { return instructions; }
        public void setInstructions(List<String> instructions) {
            this.instructions = instructions == null ? new ArrayList<>() : instructions;
        }
    }
}
