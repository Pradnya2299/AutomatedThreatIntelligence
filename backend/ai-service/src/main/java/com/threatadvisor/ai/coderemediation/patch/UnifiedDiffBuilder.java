package com.threatadvisor.ai.coderemediation.patch;

import java.util.ArrayList;
import java.util.List;

public final class UnifiedDiffBuilder {

    private UnifiedDiffBuilder() {
    }

    public static String build(String path, String before, String after) {
        List<String> oldLines = split(before);
        List<String> newLines = split(after);
        StringBuilder out = new StringBuilder();
        out.append("FILE: ").append(path).append('\n');
        int max = Math.max(oldLines.size(), newLines.size());
        for (int i = 0; i < max; i++) {
            String oldLine = i < oldLines.size() ? oldLines.get(i) : null;
            String newLine = i < newLines.size() ? newLines.get(i) : null;
            if (oldLine != null && oldLine.equals(newLine)) {
                out.append(' ').append(oldLine).append('\n');
            } else {
                if (oldLine != null) {
                    out.append('-').append(oldLine).append('\n');
                }
                if (newLine != null) {
                    out.append('+').append(newLine).append('\n');
                }
            }
        }
        return out.toString();
    }

    public static int added(String before, String after) {
        return Math.max(0, split(after).size() - split(before).size());
    }

    public static int deleted(String before, String after) {
        return Math.max(0, split(before).size() - split(after).size());
    }

    private static List<String> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        String[] parts = text.split("\\R", -1);
        List<String> lines = new ArrayList<>(parts.length);
        for (String part : parts) {
            lines.add(part);
        }
        return lines;
    }
}
