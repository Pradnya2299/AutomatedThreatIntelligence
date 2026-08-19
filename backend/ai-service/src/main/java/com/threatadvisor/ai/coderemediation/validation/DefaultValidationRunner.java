package com.threatadvisor.ai.coderemediation.validation;

import com.threatadvisor.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultValidationRunner implements ValidationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultValidationRunner.class);
    private static final int MAX_OUTPUT = 8000;

    private final AiProperties properties;

    public DefaultValidationRunner(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<ValidationOutcome> run(Path workspace, List<String[]> commands) {
        List<ValidationOutcome> outcomes = new ArrayList<>();
        if (commands.isEmpty()) {
            outcomes.add(new ValidationOutcome("none", null, 0, "No detected build/test commands", null, "SKIPPED"));
            return outcomes;
        }
        if (properties.getCodeRemediation().isSkipHostBuilds()) {
            for (String[] command : commands) {
                outcomes.add(new ValidationOutcome(
                        String.join(" ", command),
                        null,
                        0,
                        "Host builds skipped (AI_SKIP_HOST_BUILDS / demo). Command was detected, not executed.",
                        null,
                        "SKIPPED_DEMO"));
            }
            return outcomes;
        }
        for (String[] command : commands) {
            outcomes.add(execute(workspace, command));
        }
        return outcomes;
    }

    private ValidationOutcome execute(Path workspace, String[] command) {
        long started = System.nanoTime();
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspace.toFile());
        pb.redirectErrorStream(false);
        pb.environment().keySet().removeIf(k -> k.toLowerCase().contains("token")
                || k.toLowerCase().contains("secret")
                || k.toLowerCase().contains("password")
                || k.toLowerCase().contains("key"));
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return new ValidationOutcome(String.join(" ", command), 124,
                        elapsedMs(started), "timed out", "timed out", "FAILED");
            }
            String stdout = truncate(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            String stderr = truncate(new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            int code = process.exitValue();
            return new ValidationOutcome(
                    String.join(" ", command),
                    code,
                    elapsedMs(started),
                    redact(stdout),
                    redact(stderr),
                    code == 0 ? "PASSED" : "FAILED");
        } catch (Exception ex) {
            log.warn("Validation command unavailable: {}", ex.getMessage());
            return new ValidationOutcome(
                    String.join(" ", command),
                    null,
                    elapsedMs(started),
                    "Command not executed: " + ex.getClass().getSimpleName(),
                    null,
                    "COMMAND_UNAVAILABLE");
        }
    }

    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_OUTPUT ? text : text.substring(0, MAX_OUTPUT) + "…";
    }

    static String redact(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?i)(token|secret|password|api[_-]?key)\\s*[:=]\\s*\\S+", "$1=***");
    }
}
