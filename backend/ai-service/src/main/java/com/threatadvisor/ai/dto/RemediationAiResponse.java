package com.threatadvisor.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemediationAiResponse {

    @NotBlank
    private String summary;
    @NotBlank
    @Pattern(regexp = "IMMEDIATE|URGENT|SCHEDULED")
    private String priority;
    @NotBlank
    private String recommendedAction;
    private String targetVersion;
    @NotNull
    private List<String> affectedComponents = new ArrayList<>();
    @NotNull
    private List<String> prerequisites = new ArrayList<>();
    @NotNull
    private List<String> implementationSteps = new ArrayList<>();
    @NotNull
    private List<String> validationSteps = new ArrayList<>();
    @NotBlank
    private String rollbackPlan;
    @NotNull
    private Boolean downtimeExpected;
    @NotBlank
    private String reasoning;
    @NotNull
    private List<String> references = new ArrayList<>();

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public List<String> getAffectedComponents() {
        return affectedComponents;
    }

    public void setAffectedComponents(List<String> affectedComponents) {
        this.affectedComponents = affectedComponents == null ? new ArrayList<>() : affectedComponents;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites == null ? new ArrayList<>() : prerequisites;
    }

    public List<String> getImplementationSteps() {
        return implementationSteps;
    }

    public void setImplementationSteps(List<String> implementationSteps) {
        this.implementationSteps = implementationSteps == null ? new ArrayList<>() : implementationSteps;
    }

    public List<String> getValidationSteps() {
        return validationSteps;
    }

    public void setValidationSteps(List<String> validationSteps) {
        this.validationSteps = validationSteps == null ? new ArrayList<>() : validationSteps;
    }

    public String getRollbackPlan() {
        return rollbackPlan;
    }

    public void setRollbackPlan(String rollbackPlan) {
        this.rollbackPlan = rollbackPlan;
    }

    public Boolean getDowntimeExpected() {
        return downtimeExpected;
    }

    public void setDowntimeExpected(Boolean downtimeExpected) {
        this.downtimeExpected = downtimeExpected;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references == null ? new ArrayList<>() : references;
    }
}
