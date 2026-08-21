package com.threatadvisor.ai.llm;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.domain.RemediationContext;
import com.threatadvisor.ai.dto.RemediationAiResponse;
import com.threatadvisor.ai.prompt.PromptBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ai.demo-mode", havingValue = "false")
public class OpenAiRemediationService implements AiRemediationService {

    private final OpenAIClient client;
    private final AiProperties properties;

    public OpenAiRemediationService(OpenAIClient client, AiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public RemediationAiResponse generate(RemediationContext context) {
        StructuredChatCompletionCreateParams<RemediationAiResponse> params = ChatCompletionCreateParams.builder()
                .model(properties.getChatModel())
                .addSystemMessage(PromptBuilder.SYSTEM_PROMPT)
                .addUserMessage(PromptBuilder.userPrompt(context))
                .responseFormat(RemediationAiResponse.class)
                .build();
        StructuredChatCompletion<RemediationAiResponse> completion = client.chat().completions().create(params);
        return completion.choices().getFirst().message().content()
                .orElseThrow(() -> new IllegalStateException("OpenAI returned empty structured content"));
    }
}
