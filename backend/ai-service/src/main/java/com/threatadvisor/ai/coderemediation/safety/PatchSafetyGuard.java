package com.threatadvisor.ai.coderemediation.safety;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PatchSafetyGuard {

    public static final int DEFAULT_MAX_FILES = 20;
    public static final int DEFAULT_MAX_LINES = 400;
    public static final Set<String> DEFAULT_EXTENSIONS = Set.of(
            "xml", "gradle", "kts", "json", "yml", "yaml", "txt", "toml", "lock",
            "java", "js", "ts", "tsx", "py", "properties", "conf", "cfg", "md",
            "dockerfile", "tf");

    private static final Set<String> PROTECTED_NAMES = Set.of(
            ".env", "id_rsa", "id_ed25519", "credentials", "secrets.yaml", "secrets.yml");

    private PatchSafetyGuard() {
    }

    public static List<String> violations(
            List<FileChange> changes,
            int maxFiles,
            int maxLines,
            Set<String> allowedExtensions) {
        List<String> violations = new ArrayList<>();
        if (changes.size() > maxFiles) {
            violations.add("Changed file count " + changes.size() + " exceeds AI_MAX_CHANGED_FILES=" + maxFiles);
        }
        int lines = 0;
        for (FileChange change : changes) {
            lines += countLines(change.before()) + countLines(change.after());
            String name = fileName(change.path()).toLowerCase(Locale.ROOT);
            if (PROTECTED_NAMES.contains(name) || name.endsWith(".pem") || name.endsWith(".key")) {
                violations.add("Protected file modified: " + change.path());
            }
            if (looksBinary(change.after()) || looksBinary(change.before())) {
                violations.add("Binary file modified: " + change.path());
            }
            String ext = extension(change.path());
            if (!allowedExtensions.contains(ext) && !"dockerfile".equals(name)) {
                violations.add("File extension not allowed: " + change.path());
            }
            String blob = (change.before() == null ? "" : change.before()) + "\n"
                    + (change.after() == null ? "" : change.after());
            if (containsSecret(blob)) {
                violations.add("Possible secret or credential in diff: " + change.path());
            }
        }
        if (lines > maxLines) {
            violations.add("Changed line volume " + lines + " exceeds AI_MAX_CHANGED_LINES=" + maxLines);
        }
        return violations;
    }

    public static boolean containsSecret(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return text.contains("BEGIN PRIVATE KEY")
                || text.contains("BEGIN RSA PRIVATE KEY")
                || text.contains("AKIA")
                || lower.contains("ghp_")
                || lower.contains("xoxb-")
                || lower.matches("(?s).*password\\s*[:=]\\s*['\\\"][^'\\\"]{8,}['\\\"].*")
                || lower.matches("(?s).*secret\\s*[:=]\\s*['\\\"][^'\\\"]{8,}['\\\"].*")
                || lower.matches("(?s).*api[_-]?key\\s*[:=]\\s*['\\\"][^'\\\"]{8,}['\\\"].*");
    }

    private static boolean looksBinary(String content) {
        if (content == null) {
            return false;
        }
        for (int i = 0; i < Math.min(content.length(), 2048); i++) {
            if (content.charAt(i) == 0) {
                return true;
            }
        }
        return false;
    }

    private static int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\\R", -1).length;
    }

    private static String fileName(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String extension(String path) {
        String name = fileName(path).toLowerCase(Locale.ROOT);
        if ("dockerfile".equals(name) || name.startsWith("dockerfile")) {
            return "dockerfile";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    public record FileChange(String path, String before, String after) {
    }
}
