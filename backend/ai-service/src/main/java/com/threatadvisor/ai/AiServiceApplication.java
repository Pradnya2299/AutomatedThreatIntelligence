package com.threatadvisor.ai;

import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.config.DotenvEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableConfigurationProperties(AiProperties.class)
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AiServiceApplication.class);
        application.addInitializers((ApplicationContextInitializer<ConfigurableApplicationContext>) context ->
                DotenvEnvironmentPostProcessor.apply(context.getEnvironment()));
        application.run(args);
    }
}
