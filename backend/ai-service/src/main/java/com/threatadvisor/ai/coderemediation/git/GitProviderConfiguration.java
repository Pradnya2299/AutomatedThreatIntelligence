package com.threatadvisor.ai.coderemediation.git;

import com.threatadvisor.ai.config.AiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitProviderConfiguration {

    @Bean
    public GitProvider gitProvider(AiProperties properties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        if (properties.getGit().isEnabled()
                && properties.getGit().getToken() != null
                && !properties.getGit().getToken().isBlank()) {
            return new GitHubGitProvider(properties, objectMapper);
        }
        return new LocalWorkspaceGitProvider();
    }
}
