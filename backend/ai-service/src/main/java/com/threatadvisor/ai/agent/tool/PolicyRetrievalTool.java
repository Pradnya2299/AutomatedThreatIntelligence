package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;

public interface PolicyRetrievalTool {
    KnowledgeSearchResult retrievePolicies(String query);
}
