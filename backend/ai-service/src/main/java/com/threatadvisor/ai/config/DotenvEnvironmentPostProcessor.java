package com.threatadvisor.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads repo-root {@code .env} so OPENAI_API_KEY is visible to Spring.
 * File values override empty shell variables (common when an old empty key was exported).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    public static final String PROPERTY_SOURCE_NAME = "threatAdvisorDotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        apply(environment);
    }

    public static void apply(ConfigurableEnvironment environment) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Path file = findEnvFile();
        if (file == null) {
            log.warn("operation=dotenv.missing no .env found under {} (OPENAI_API_KEY will be empty unless exported)",
                    Path.of("").toAbsolutePath());
            return;
        }
        Map<String, Object> values = read(file);
        if (values.isEmpty()) {
            log.warn("operation=dotenv.empty path={}", file.toAbsolutePath());
            return;
        }
        Object rawKey = values.get("OPENAI_API_KEY");
        boolean keyPresent = rawKey instanceof String s && !s.isBlank();
        if (keyPresent) {
            values.put("ai.api-key", rawKey);
            values.put("openai.api-key", rawKey);
        }
        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
            environment.getPropertySources().addAfter(
                    StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, source);
        } else {
            environment.getPropertySources().addFirst(source);
        }
        log.info("operation=dotenv.loaded path={} openaiKeyPresent={}", file.toAbsolutePath(), keyPresent);
    }

    static Path findEnvFile() {
        for (Path start : searchRoots()) {
            Path cwd = start;
            for (int i = 0; i < 8 && cwd != null; i++) {
                Path candidate = cwd.resolve(".env");
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
                cwd = cwd.getParent();
            }
        }
        return null;
    }

    private static Path[] searchRoots() {
        return new Path[] {
                Path.of("").toAbsolutePath(),
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath(),
                Path.of(System.getProperty("basedir", ".")).toAbsolutePath()
        };
    }

    static Map<String, Object> read(Path file) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.replace("\uFEFF", "").strip();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("export ")) {
                    line = line.substring(7).strip();
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    continue;
                }
                String value = stripQuotes(line.substring(eq + 1).trim());
                Object existing = values.get(key);
                if (value.isBlank() && existing instanceof String prev && !prev.isBlank()) {
                    continue;
                }
                values.put(key, value);
            }
        } catch (IOException ex) {
            log.warn("operation=dotenv.read-failed path={} message={}", file, ex.getMessage());
            return Map.of();
        }
        return values;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
