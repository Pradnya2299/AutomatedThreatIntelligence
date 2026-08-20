package com.threatadvisor.ai.coderemediation.llm;

public interface CodeRemediationLlm {

    CodeAnalysisResponse analyze(String userPrompt);

    LlmPatchPlan plan(String userPrompt);

    GeneratedPatch generate(String userPrompt);

    String mode();

    String modelName();
}
