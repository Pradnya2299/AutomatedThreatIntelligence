package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.coderemediation.git.GitProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CodeContextSelector {

    public static final int MAX_FILE_CHARS = 8000;

    public static final List<String> CANDIDATES = List.of(
            "pom.xml",
            "build.gradle",
            "build.gradle.kts",
            "package.json",
            "Dockerfile",
            "src/main/java/com/northwind/InsecureHash.java"
    );

    public record WorkspaceFile(String path, String content) {
    }

    public List<WorkspaceFile> select(Path workspace, GitProvider git, String productHint) {
        Map<String, WorkspaceFile> files = new LinkedHashMap<>();
        for (String path : CANDIDATES) {
            String content = git.getFile(workspace, path);
            if (content != null) {
                files.put(path, new WorkspaceFile(path, trim(content)));
            }
        }
        if (productHint != null && !productHint.isBlank()) {
            for (String hit : git.searchCode(workspace, productHint)) {
                if (files.containsKey(hit) || hit.contains(".git/")) {
                    continue;
                }
                if (files.size() >= 12) {
                    break;
                }
                String content = git.getFile(workspace, hit);
                if (content != null) {
                    files.put(hit, new WorkspaceFile(hit, trim(content)));
                }
            }
        }
        return new ArrayList<>(files.values());
    }

    public static boolean allowed(String path, List<WorkspaceFile> context) {
        if (path == null) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        if (normalized.contains("..")) {
            return false;
        }
        return context.stream().anyMatch(file -> file.path().equals(normalized));
    }

    private static String trim(String content) {
        if (content.length() <= MAX_FILE_CHARS) {
            return content;
        }
        return content.substring(0, MAX_FILE_CHARS) + "\n/* truncated */\n";
    }

    public static String formatForPrompt(List<WorkspaceFile> files) {
        StringBuilder out = new StringBuilder();
        for (WorkspaceFile file : files) {
            out.append("--- FILE: ").append(file.path()).append('\n');
            out.append(file.content());
            out.append("--- END FILE: ").append(file.path()).append('\n');
        }
        return out.toString();
    }

    public static String joinHints(String product) {
        return product == null ? "" : product.toLowerCase(Locale.ROOT);
    }
}
