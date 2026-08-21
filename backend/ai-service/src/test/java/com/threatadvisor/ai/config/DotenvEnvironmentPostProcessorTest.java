package com.threatadvisor.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DotenvEnvironmentPostProcessorTest {

    @TempDir
    Path temp;

    @Test
    void lastNonBlankOpenAiKeyWins() throws Exception {
        Path env = temp.resolve(".env");
        Files.writeString(env, """
                OPENAI_API_KEY=
                OPENAI_API_KEY=sk-test-key
                """, StandardCharsets.UTF_8);
        Map<String, Object> values = DotenvEnvironmentPostProcessor.read(env);
        assertEquals("sk-test-key", values.get("OPENAI_API_KEY"));
    }

    @Test
    void blankLineDoesNotEraseEarlierKey() throws Exception {
        Path env = temp.resolve(".env");
        Files.writeString(env, """
                OPENAI_API_KEY=sk-keep
                OPENAI_API_KEY=
                """, StandardCharsets.UTF_8);
        Map<String, Object> values = DotenvEnvironmentPostProcessor.read(env);
        assertEquals("sk-keep", values.get("OPENAI_API_KEY"));
        assertFalse(((String) values.get("OPENAI_API_KEY")).isBlank());
    }
}
