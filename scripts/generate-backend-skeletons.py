#!/usr/bin/env python3
"""Generate Phase 1 Spring Boot service skeletons."""
from pathlib import Path

ROOT = Path("/workspace/backend")

SERVICES = [
    {
        "module": "api-service",
        "pkg": "api",
        "app": "ApiServiceApplication",
        "name": "api-service",
        "port": 8080,
        "flyway": True,
        "description": "Synchronous SecOps REST API, authn/z, Flyway owner, dashboard BFF.",
    },
    {
        "module": "ingestion-service",
        "pkg": "ingestion",
        "app": "IngestionServiceApplication",
        "name": "ingestion-service",
        "port": 8081,
        "flyway": False,
        "description": "CVE intake, normalization, enrichment (engines in later phases).",
    },
    {
        "module": "correlation-service",
        "pkg": "correlation",
        "app": "CorrelationServiceApplication",
        "name": "correlation-service",
        "port": 8082,
        "flyway": False,
        "description": "Deterministic CVE-to-asset correlation (engine in later phases).",
    },
    {
        "module": "risk-service",
        "pkg": "risk",
        "app": "RiskServiceApplication",
        "name": "risk-service",
        "port": 8083,
        "flyway": False,
        "description": "Deterministic risk engine (formula in later phases).",
    },
    {
        "module": "ai-service",
        "pkg": "ai",
        "app": "AiServiceApplication",
        "name": "ai-service",
        "port": 8084,
        "flyway": False,
        "description": "Tool-based AI orchestrator and RAG consumer (OpenAI in later phases).",
    },
    {
        "module": "notification-service",
        "pkg": "notification",
        "app": "NotificationServiceApplication",
        "name": "notification-service",
        "port": 8085,
        "flyway": False,
        "description": "Notification delivery and in-app records (channels in later phases).",
    },
]

POM = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.threatadvisor</groupId>
        <artifactId>threat-advisor-parent</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>{module}</artifactId>
    <name>{module}</name>
    <description>{description}</description>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
{flyway_dep}
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
"""

FLYWAY = """        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
"""

APP = """package com.threatadvisor.{pkg};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class {app} {{

    public static void main(String[] args) {{
        SpringApplication.run({app}.class, args);
    }}
}}
"""

HEALTH_CONTROLLER = """package com.threatadvisor.{pkg}.controller;

import com.threatadvisor.{pkg}.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HealthController {{

    private final String serviceName;

    public HealthController(@Value("${{spring.application.name:unknown}}") String serviceName) {{
        this.serviceName = serviceName;
    }}

    @GetMapping("/api/health")
    public HealthResponse health() {{
        return new HealthResponse("UP", serviceName, Instant.now());
    }}
}}
"""

HEALTH_DTO = """package com.threatadvisor.{pkg}.dto;

import java.time.Instant;

public record HealthResponse(String status, String service, Instant timestamp) {{
}}
"""

SECURITY = """package com.threatadvisor.{pkg}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {{

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }}
}}
"""

API_SECURITY = """package com.threatadvisor.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 1 local security: in-memory users from environment.
 * Replace with JDBC/IdP in a later phase. Roles match the product spec.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {{

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }}

    @Bean
    public PasswordEncoder passwordEncoder() {{
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }}

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${{app.security.admin.username:admin}}") String adminUser,
            @Value("${{app.security.admin.password:admin_change_me}}") String adminPassword,
            @Value("${{app.security.analyst.username:analyst}}") String analystUser,
            @Value("${{app.security.analyst.password:analyst_change_me}}") String analystPassword,
            @Value("${{app.security.manager.username:manager}}") String managerUser,
            @Value("${{app.security.manager.password:manager_change_me}}") String managerPassword,
            @Value("${{app.security.viewer.username:viewer}}") String viewerUser,
            @Value("${{app.security.viewer.password:viewer_change_me}}") String viewerPassword) {{
        return new InMemoryUserDetailsManager(
                User.withUsername(adminUser).password(encoder.encode(adminPassword)).roles("ADMIN").build(),
                User.withUsername(analystUser).password(encoder.encode(analystPassword)).roles("SECURITY_ANALYST").build(),
                User.withUsername(managerUser).password(encoder.encode(managerPassword)).roles("SECURITY_MANAGER").build(),
                User.withUsername(viewerUser).password(encoder.encode(viewerPassword)).roles("VIEWER").build()
        );
    }}
}}
"""

CORS = """package com.threatadvisor.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {{

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {{
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }}
}}
"""

FILTER = """package com.threatadvisor.{pkg}.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {{

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {{
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {{
            correlationId = UUID.randomUUID().toString();
        }}
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {{
            filterChain.doFilter(request, response);
        }} finally {{
            MDC.remove(MDC_KEY);
        }}
    }}
}}
"""

EXCEPTION = """package com.threatadvisor.{pkg}.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {{

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception ex) {{
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", "INTERNAL_ERROR",
                "message", "An unexpected error occurred",
                "correlationId", String.valueOf(MDC.get("correlationId")),
                "timestamp", Instant.now().toString()
        ));
    }}
}}
"""

PLACEHOLDERS = """package com.threatadvisor.{pkg};

/**
 * Layer packages reserved for later phases. Keep Kafka consumers and controllers thin;
 * business logic belongs in service/domain.
 */
final class PackageInfo {{
    private PackageInfo() {{
    }}
}}
"""

TEST = """package com.threatadvisor.{pkg}.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class HealthControllerTest {{

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicAndReportsUp() throws Exception {{
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").exists());
    }}
}}
"""

APPLICATION_YML = """spring:
  application:
    name: {name}
  profiles:
    active: ${{SPRING_PROFILES_ACTIVE:local}}
  datasource:
    url: ${{DATABASE_URL:jdbc:postgresql://localhost:5432/threat_advisor}}
    username: ${{POSTGRES_USER:threat_advisor}}
    password: ${{POSTGRES_PASSWORD:threat_advisor_dev_change_me}}
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: {flyway_enabled}
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: ${{KAFKA_BOOTSTRAP_SERVERS:localhost:9092}}
    listener:
      auto-startup: false
  data:
    redis:
      host: ${{REDIS_HOST:localhost}}
      port: ${{REDIS_PORT:6379}}
      password: ${{REDIS_PASSWORD:}}

server:
  port: ${{{port_env}:{port}}}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
  tracing:
    enabled: false

logging:
  pattern:
    console: '%d{{yyyy-MM-dd''T''HH:mm:ss.SSSXXX}} service={name} level=%p correlationId=%X{{correlationId:-}} eventId=%X{{eventId:-}} operation=%X{{operation:-}} %msg%n'
"""

APPLICATION_LOCAL = """# Local profile: infrastructure via docker compose, apps on the host.
spring:
  jpa:
    show-sql: false
"""

README = """# {module}

{description}

## Run

Infrastructure must be up (`docker compose up -d` from repo root).

```bash
cd backend
./mvnw -pl {module} spring-boot:run
```

Health: http://localhost:{port}/api/health  
Actuator: http://localhost:{port}/actuator/health

## Package layout

`com.threatadvisor.{pkg}` — controller, config, dto, exception, plus empty service/domain/repository/kafka packages for later phases.

Do not put business logic in controllers or Kafka consumers.
"""

LOGBACK = """<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <springProperty name="serviceName" source="spring.application.name" defaultValue="unknown"/>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}} service=${{serviceName}} level=%p correlationId=%X{{correlationId:-}} eventId=%X{{eventId:-}} %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
"""


def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)


def main():
    for svc in SERVICES:
        module = svc["module"]
        pkg = svc["pkg"]
        base = ROOT / module
        java = base / "src/main/java/com/threatadvisor" / pkg
        res = base / "src/main/resources"
        test = base / "src/test/java/com/threatadvisor" / pkg / "controller"

        flyway_dep = FLYWAY if svc["flyway"] else ""
        write(base / "pom.xml", POM.format(**svc, flyway_dep=flyway_dep))
        write(java / f"{svc['app']}.java", APP.format(**svc))
        write(java / "controller" / "HealthController.java", HEALTH_CONTROLLER.format(**svc))
        write(java / "dto" / "HealthResponse.java", HEALTH_DTO.format(**svc))
        if svc["module"] == "api-service":
            write(java / "config" / "SecurityConfig.java", API_SECURITY)
            write(java / "config" / "CorsConfig.java", CORS)
        else:
            write(java / "config" / "SecurityConfig.java", SECURITY.format(**svc))
        write(java / "config" / "CorrelationIdFilter.java", FILTER.format(**svc))
        write(java / "exception" / "GlobalExceptionHandler.java", EXCEPTION.format(**svc))
        for layer in ("service", "domain", "repository", "kafka", "mapper", "validation"):
            write(java / layer / "package-info.java", f"package com.threatadvisor.{pkg}.{layer};\n")
        write(
            res / "application.yml",
            APPLICATION_YML.format(
                **svc,
                flyway_enabled="true" if svc["flyway"] else "false",
                port_env={
                    "api-service": "API_SERVICE_PORT",
                    "ingestion-service": "INGESTION_SERVICE_PORT",
                    "correlation-service": "CORRELATION_SERVICE_PORT",
                    "risk-service": "RISK_SERVICE_PORT",
                    "ai-service": "AI_SERVICE_PORT",
                    "notification-service": "NOTIFICATION_SERVICE_PORT",
                }[module],
            ),
        )
        write(res / "application-local.yml", APPLICATION_LOCAL)
        write(res / "logback-spring.xml", LOGBACK)
        write(test / "HealthControllerTest.java", TEST.format(**svc))
        write(base / "README.md", README.format(**svc))

    print("generated services")


if __name__ == "__main__":
    main()
