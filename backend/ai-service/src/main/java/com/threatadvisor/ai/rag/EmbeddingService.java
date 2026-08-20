package com.threatadvisor.ai.rag;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;

import java.util.List;

public interface EmbeddingService {

    /**
     * Returns a 1536-dimension vector as a pgvector literal {@code [0.1,0.2,...]}.
     */
    String embed(String text);

    List<KnowledgeChunkHit> search(String query, int topK);
}
