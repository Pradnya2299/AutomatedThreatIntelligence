package com.threatadvisor.ai.coderemediation.llm;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.threatadvisor.ai.config.AiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ai.demo-mode", havingValue = "false")
public class OpenAiCodeRemediationLlm implements CodeRemediationLlm {

    static final String ANALYSIS_SYSTEM = """
            You are a code analysis agent for a SOC platform.
            Use only the supplied NVD facts, risk facts, RAG policies, and repository files.
            Never invent file paths. Never claim CVSS or risk scores. Never execute commands.
            If evidence is insufficient return remediationStrategy INSUFFICIENT_EVIDENCE or REVIEW_REQUIRED and LOW confidence.
            """;

    static final String PLAN_SYSTEM = """
            You are a patch planning agent. Produce a structured plan only. Do not write file contents.
            Only reference files listed in the prompt. Human approval is required later; you do not approve.
            """;

    static final String GENERATE_SYSTEM = """
            You are a patch generation agent. Return full proposed file contents for listed files only.
            originalContentHash must be the SHA-256 hex supplied in the prompt for that file.
            Do not include secrets, .env files, or unrelated files.
            """;

    private final OpenAIClient client;
    private final AiProperties properties;

    public OpenAiCodeRemediationLlm(OpenAIClient client, AiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public CodeAnalysisResponse analyze(String userPrompt) {
        return complete(ANALYSIS_SYSTEM, userPrompt, CodeAnalysisResponse.class);
    }

    @Override
    public LlmPatchPlan plan(String userPrompt) {
        return complete(PLAN_SYSTEM, userPrompt, LlmPatchPlan.class);
    }

    @Override
    public GeneratedPatch generate(String userPrompt) {
        return complete(GENERATE_SYSTEM, userPrompt, GeneratedPatch.class);
    }

    @Override
    public String mode() {
        return "LLM_POWERED";
    }

    @Override
    public String modelName() {
        return properties.getChatModel();
    }

    private <T> T complete(String system, String user, Class<T> type) {
        try {
            StructuredChatCompletionCreateParams<T> params = ChatCompletionCreateParams.builder()
                    .model(properties.getChatModel())
                    .addSystemMessage(system)
                    .addUserMessage(user)
                    .responseFormat(type)
                    .build();
            StructuredChatCompletion<T> completion = client.chat().completions().create(params);
            return completion.choices().getFirst().message().content()
                    .orElseThrow(() -> new LlmUnavailableException("OpenAI returned empty structured content"));
        } catch (LlmUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException("OpenAI code-remediation call failed", ex);
        }
    }
}
