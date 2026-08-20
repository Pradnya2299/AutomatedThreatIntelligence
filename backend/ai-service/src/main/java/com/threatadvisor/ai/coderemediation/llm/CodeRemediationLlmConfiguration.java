package com.threatadvisor.ai.coderemediation.llm;

import com.openai.client.OpenAIClient;
import com.threatadvisor.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodeRemediationLlmConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CodeRemediationLlmConfiguration.class);

    @Bean
    public CodeRemediationLlm codeRemediationLlm(AiProperties properties, ObjectProvider<OpenAIClient> clients) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.info("operation=code-remediation.llm mode=DEMO_MODE reason=OPENAI_API_KEY_empty");
            return new DemoCodeRemediationLlm();
        }
        OpenAIClient client = clients.getIfAvailable();
        if (client == null) {
            log.warn("operation=code-remediation.llm mode=DEMO_MODE reason=openai_client_missing");
            return new DemoCodeRemediationLlm();
        }
        log.info("operation=code-remediation.llm mode=LLM_POWERED model={}", properties.getChatModel());
        return new OpenAiCodeRemediationLlm(client, properties);
    }
}
