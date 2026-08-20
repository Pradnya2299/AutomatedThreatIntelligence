package com.threatadvisor.ai.agent.common;

import com.threatadvisor.ai.agent.dto.AffectedAssetMatch;
import com.threatadvisor.ai.agent.dto.AssetInvestigationResult;
import com.threatadvisor.ai.agent.dto.ThreatIntelligenceResult;
import com.threatadvisor.ai.domain.Vulnerability;

import java.util.List;
import java.util.regex.Pattern;

public final class InvestigationConfidence {

    private static final Pattern VERSION = Pattern.compile("\\d+\\.\\d+");

    private InvestigationConfidence() {
    }

    public static Confidence threat(Vulnerability vulnerability, ThreatIntelligenceResult result) {
        boolean hasCvss = vulnerability != null && vulnerability.getCvssScore() != null
                || (result != null && result.cvssScore() != null);
        boolean hasProducts = result != null && result.affectedProducts() != null && !result.affectedProducts().isEmpty();
        boolean unknownExploit = result != null && "UNKNOWN_FROM_SOURCE".equals(result.exploitability());
        if (!hasCvss && !hasProducts) {
            return Confidence.LOW;
        }
        if (!hasCvss || !hasProducts) {
            return Confidence.LOW;
        }
        if (unknownExploit) {
            return Confidence.MEDIUM;
        }
        return Confidence.HIGH;
    }

    public static Confidence assets(AssetInvestigationResult result) {
        if (result == null) {
            return Confidence.LOW;
        }
        if (!result.affected()) {
            return Confidence.HIGH;
        }
        List<AffectedAssetMatch> matches = result.assets();
        if (matches == null || matches.isEmpty()) {
            return Confidence.LOW;
        }
        boolean anyLow = false;
        boolean anyMissingVersion = false;
        boolean allHigh = true;
        for (AffectedAssetMatch match : matches) {
            if (!"HIGH".equalsIgnoreCase(match.matchConfidence())) {
                allHigh = false;
            }
            if ("LOW".equalsIgnoreCase(match.matchConfidence()) || match.matchType() == null || match.matchType().isBlank()) {
                anyLow = true;
            }
            if (!hasVersion(match)) {
                anyMissingVersion = true;
            }
        }
        if (anyLow || anyMissingVersion) {
            return Confidence.LOW;
        }
        if (allHigh) {
            return Confidence.HIGH;
        }
        return Confidence.MEDIUM;
    }

    public static boolean hasVersion(AffectedAssetMatch match) {
        if (match == null) {
            return false;
        }
        if ("EXACT_VERSION_MATCH".equalsIgnoreCase(match.matchType())) {
            return true;
        }
        String reason = match.matchReason() == null ? "" : match.matchReason();
        return VERSION.matcher(reason).find();
    }

    public static Confidence min(Confidence left, Confidence right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (left == Confidence.LOW || right == Confidence.LOW) {
            return Confidence.LOW;
        }
        if (left == Confidence.MEDIUM || right == Confidence.MEDIUM) {
            return Confidence.MEDIUM;
        }
        return Confidence.HIGH;
    }
}
