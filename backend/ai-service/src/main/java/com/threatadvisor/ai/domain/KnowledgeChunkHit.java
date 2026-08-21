package com.threatadvisor.ai.domain;

import java.util.UUID;

public record KnowledgeChunkHit(
        UUID chunkId,
        UUID documentId,
        String title,
        String source,
        String documentType,
        String content,
        double distance
) {
}
