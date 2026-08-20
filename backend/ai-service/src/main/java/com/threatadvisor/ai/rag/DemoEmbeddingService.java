package com.threatadvisor.ai.rag;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic 1536-d hashed bag-of-words vectors for AI_DEMO_MODE.
 * Same text always yields the same vector. Not an OpenAI embedding.
 */
@Service
@ConditionalOnProperty(name = "ai.demo-mode", havingValue = "true", matchIfMissing = true)
public class DemoEmbeddingService implements EmbeddingService {

    public static final int DIMENSIONS = 1536;

    private final VectorSearchRepository vectors;

    public DemoEmbeddingService(VectorSearchRepository vectors) {
        this.vectors = vectors;
    }

    @Override
    public String embed(String text) {
        return toLiteral(hashVector(text));
    }

    @Override
    public List<KnowledgeChunkHit> search(String query, int topK) {
        return vectors.search(embed(query), topK);
    }

    static float[] hashVector(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            vector[0] = 1f;
            return vector;
        }
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int index = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[index] += 1f;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            vector[0] = 1f;
            return vector;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
        return vector;
    }

    static String toLiteral(float[] vector) {
        StringBuilder out = new StringBuilder(vector.length * 8);
        out.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(vector[i]);
        }
        out.append(']');
        return out.toString();
    }
}
