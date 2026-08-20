package com.threatadvisor.ai.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.domain.Asset;
import com.threatadvisor.ai.domain.Finding;
import com.threatadvisor.ai.repository.AssetRepository;
import com.threatadvisor.ai.repository.FindingRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DatabaseAffectedAssetLookupTool implements AffectedAssetLookupTool {

    private final FindingRepository findings;
    private final AssetRepository assets;
    private final ObjectMapper objectMapper;

    public DatabaseAffectedAssetLookupTool(
            FindingRepository findings, AssetRepository assets, ObjectMapper objectMapper) {
        this.findings = findings;
        this.assets = assets;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AffectedAssetMatch> lookup(UUID vulnerabilityId) {
        List<AffectedAssetMatch> matches = new ArrayList<>();
        for (Finding finding : findings.findByVulnerabilityId(vulnerabilityId)) {
            Asset asset = assets.findById(finding.getAssetId()).orElse(null);
            matches.add(new AffectedAssetMatch(
                    finding.getId(),
                    finding.getAssetId(),
                    asset == null ? null : asset.getHostname(),
                    asset == null ? null : asset.getEnvironment(),
                    asset == null ? null : asset.getBusinessCriticality(),
                    asset != null && asset.isInternetExposure(),
                    finding.getMatchType(),
                    finding.getMatchConfidence(),
                    explanationText(finding.getMatchExplanation())));
        }
        return List.copyOf(matches);
    }

    private String explanationText(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("text")) {
                return node.get("text").asText();
            }
            return json;
        } catch (Exception ex) {
            return json;
        }
    }
}
