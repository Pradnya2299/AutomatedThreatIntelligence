package com.threatadvisor.correlation;

import com.threatadvisor.correlation.config.CorrelationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CorrelationProperties.class)
public class CorrelationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorrelationServiceApplication.class, args);
    }
}
