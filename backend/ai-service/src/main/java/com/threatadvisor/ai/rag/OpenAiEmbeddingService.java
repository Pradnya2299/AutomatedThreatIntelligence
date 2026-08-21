package com.threatadvisor.ai.rag;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.domain.KnowledgeChunkHit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.demo-mode", havingValue = "false")
public class OpenAiEmbeddingService implements EmbeddingService {

    private final OpenAIClient client;
    private final AiProperties properties;
    private final VectorSearchRepository vectors;

    public OpenAiEmbeddingService(OpenAIClient client, AiProperties properties, VectorSearchRepository vectors) {
        this.client = client;
        this.properties = properties;
        this.vectors = vectors;
    }

    @Override
    public String embed(String text) {
        CreateEmbeddingResponse response;
        try {
            response = client.embeddings().create(
                    EmbeddingCreateParams.builder()
                            .model(properties.getEmbeddingModel())
                            .input(text == null ? "" : text)
                            .build());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "OpenAI embeddings failed model=" + properties.getEmbeddingModel(), ex);
        }
        List<Float> values = response.data().getFirst().embedding();
        StringBuilder literal = new StringBuilder(values.size() * 8);
        literal.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(values.get(i).doubleValue());
        }
        literal.append(']');
        return literal.toString();
    }

    @Override
    public List<KnowledgeChunkHit> search(String query, int topK) {
        return vectors.search(embed(query), topK);
    }
}
