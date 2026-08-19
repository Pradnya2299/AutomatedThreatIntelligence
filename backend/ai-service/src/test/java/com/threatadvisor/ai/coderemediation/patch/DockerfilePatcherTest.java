package com.threatadvisor.ai.coderemediation.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerfilePatcherTest {

    @Test
    void replacesFromLine() {
        String docker = "FROM openjdk:8u222-jdk\nWORKDIR /app\n";
        assertEquals("openjdk:8u222-jdk", DockerfilePatcher.currentFrom(docker));
        String patched = DockerfilePatcher.replaceFrom(docker, "eclipse-temurin:17-jre");
        assertTrue(patched.startsWith("FROM eclipse-temurin:17-jre\n"));
    }
}
