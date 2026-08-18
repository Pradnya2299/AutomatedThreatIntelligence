package com.threatadvisor.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void splitsWithOverlap() {
        String body = "a".repeat(500);
        List<String> chunks = TextChunker.chunk(body, 200, 50);
        assertTrue(chunks.size() >= 3);
        assertEquals(200, chunks.getFirst().length());
    }

    @Test
    void shortDocumentIsSingleChunk() {
        assertEquals(List.of("hello policy"), TextChunker.chunk("hello policy", 2000, 200));
    }
}
