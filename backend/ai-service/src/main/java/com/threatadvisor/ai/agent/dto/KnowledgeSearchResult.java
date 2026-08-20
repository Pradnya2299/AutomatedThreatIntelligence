package com.threatadvisor.ai.agent.dto;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;

import java.util.List;

public record KnowledgeSearchResult(List<KnowledgeChunkHit> hits) {
}
