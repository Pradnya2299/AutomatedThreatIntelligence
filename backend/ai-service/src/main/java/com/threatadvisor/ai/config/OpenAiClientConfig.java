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
        String projectId = properties.getProjectId();
        if (projectId == null || projectId.isBlank()) {
            projectId = environment.getProperty("OPENAI_PROJECT_ID");
        }
        var builder = OpenAIOkHttpClient.builder().apiKey(key.trim());
        if (projectId != null && !projectId.isBlank()) {
            builder.project(projectId.trim());
        }
        log.info("operation=openai.client OpenAI client enabled model={} projectSet={}",
                properties.getChatModel(), projectId != null && !projectId.isBlank());
        return builder.build();
    }
}
