package com.threatadvisor.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Character chunking approximating 500–1000 tokens (about 4 chars/token).
 * Default 2000 characters (~500 tokens) with 200-character overlap.
 */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunk(String body, int size, int overlap) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        String text = body.trim();
        int chunkSize = Math.max(200, size);
        int stepOverlap = Math.max(0, Math.min(overlap, chunkSize / 2));
        if (text.length() <= chunkSize) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
            start = end - stepOverlap;
        }
        return chunks.stream().filter(s -> !s.isBlank()).toList();
    }
}
