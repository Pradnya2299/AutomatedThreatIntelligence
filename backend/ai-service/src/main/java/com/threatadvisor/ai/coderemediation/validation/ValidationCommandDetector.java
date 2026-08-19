package com.threatadvisor.ai.coderemediation.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ValidationCommandDetector {

    private ValidationCommandDetector() {
    }

    public static List<String[]> detect(Path workspace) {
        List<String[]> commands = new ArrayList<>();
        if (Files.exists(workspace.resolve("pom.xml"))) {
            commands.add(new String[] {"mvn", "-q", "-DskipTests=false", "test"});
            commands.add(new String[] {"mvn", "-q", "package", "-DskipTests"});
        }
        if (Files.exists(workspace.resolve("gradlew"))) {
            commands.add(new String[] {"./gradlew", "test"});
            commands.add(new String[] {"./gradlew", "build", "-x", "test"});
        } else if (Files.exists(workspace.resolve("build.gradle")) || Files.exists(workspace.resolve("build.gradle.kts"))) {
            commands.add(new String[] {"gradle", "test"});
        }
        if (Files.exists(workspace.resolve("package.json"))) {
            commands.add(new String[] {"npm", "test"});
            commands.add(new String[] {"npm", "audit"});
        }
        if (Files.exists(workspace.resolve("requirements.txt"))
                || Files.exists(workspace.resolve("pyproject.toml"))) {
            commands.add(new String[] {"pytest"});
        }
        if (Files.exists(workspace.resolve("Dockerfile"))) {
            commands.add(new String[] {"docker", "build", "."});
        }
        return commands;
    }
}
