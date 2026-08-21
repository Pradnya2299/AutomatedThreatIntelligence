package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;
import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.rag.EmbeddingService;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingKnowledgeTools implements KnowledgeSearchTool, PolicyRetrievalTool {

    private final EmbeddingService embeddings;
    private final AiProperties properties;

    public EmbeddingKnowledgeTools(EmbeddingService embeddings, AiProperties properties) {
        this.embeddings = embeddings;
        this.properties = properties;
    }

    @Override
    public KnowledgeSearchResult search(String query) {
        return new KnowledgeSearchResult(embeddings.search(query, properties.getTopK()));
    }

    @Override
    public KnowledgeSearchResult retrievePolicies(String query) {
        String policyQuery = query == null ? "security patch policy" : "policy " + query;
        return search(policyQuery);
    }
}
