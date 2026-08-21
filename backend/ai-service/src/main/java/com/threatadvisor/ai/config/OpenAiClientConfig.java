package com.threatadvisor.ai.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientConfig.class);

    @Bean
    public OpenAIClient openAIClient(AiProperties properties, org.springframework.core.env.Environment environment) {
        String key = properties.getApiKey();
        if (key == null || key.isBlank()) {
            key = environment.getProperty("OPENAI_API_KEY");
        }
        if (key == null || key.isBlank()) {
            return null;
        }
        log.info("operation=openai.client OpenAI client enabled model={}", properties.getChatModel());
        return OpenAIOkHttpClient.builder().apiKey(key.trim()).build();
    }
}
