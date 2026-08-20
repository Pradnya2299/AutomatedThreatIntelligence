package com.threatadvisor.ai.agent.tool;

import com.threatadvisor.ai.agent.dto.KnowledgeSearchResult;

public interface KnowledgeSearchTool {
    KnowledgeSearchResult search(String query);
}
