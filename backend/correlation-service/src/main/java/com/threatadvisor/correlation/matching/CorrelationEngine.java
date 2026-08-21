package com.threatadvisor.correlation.matching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic CVE ↔ installed-software matcher. No LLM. Conservative by default:
 * vendor or product mismatch never yields a finding; wildcard CPE versions without
 * range columns never yield a finding.
 */
public final class CorrelationEngine {

    public List<AssetMatch> evaluate(String cveId, List<CpeConstraint> cpes, List<InstalledSoftware> candidates) {
        Map<UUID, AssetMatch> bestByAsset = new LinkedHashMap<>();
        for (InstalledSoftware software : candidates) {
            for (CpeConstraint cpe : cpes) {
                match(cveId, software, cpe).ifPresent(result -> {
                    AssetMatch existing = bestByAsset.get(software.assetId());
                    if (existing == null || rank(result) > rank(existing)) {
                        bestByAsset.put(software.assetId(), result);
                    }
                });
            }
        }
        List<AssetMatch> out = new ArrayList<>(bestByAsset.values());
        out.sort(Comparator.comparing(m -> m.hostname() == null ? "" : m.hostname()));
        return out;
    }

    public Optional<AssetMatch> match(String cveId, InstalledSoftware software, CpeConstraint cpe) {
        String cpeVendor = cpe.vendorOrParsed();
        String cpeProduct = cpe.productOrParsed();
        if (!IdentityNormalizer.keysEqual(cpeVendor, software.vendor())) {
            return Optional.empty();
        }
        if (IdentityNormalizer.isBlank(cpeProduct) || IdentityNormalizer.isBlank(software.product())) {
            return Optional.empty();
        }
        if (!IdentityNormalizer.keysEqual(cpeProduct, software.product())) {
            return Optional.empty();
        }

        CpeIdentifier parsed = CpeIdentifier.parse(cpe.cpe()).orElse(null);
        VersionRange range = cpe.range();
        boolean hasRange = range.hasBound();
        boolean hasInstalledVersion = VersionComparator.isComparable(software.version());
        String exactCpeVersion = parsed != null && parsed.versionIsExact() ? parsed.version() : null;

        if (hasRange) {
            if (!hasInstalledVersion) {
                return Optional.of(partial(cveId, software, cpe));
            }
            if (!range.contains(software.version())) {
                return Optional.empty();
            }
            if (exactCpeVersion != null && !VersionComparator.equal(software.version(), exactCpeVersion)) {
                return Optional.empty();
            }
            return Optional.of(new AssetMatch(
                    software.assetId(),
                    software.organizationId(),
                    software.hostname(),
                    cveId,
                    software.vendor(),
                    software.product(),
                    software.version(),
                    cpe,
                    MatchType.VERSION_RANGE_MATCH,
                    MatchConfidence.HIGH,
                    MatchExplanationFormatter.versionRange(software, cveId, cpe)));
        }

        if (exactCpeVersion != null) {
            if (!hasInstalledVersion) {
                return Optional.of(partial(cveId, software, cpe));
            }
            if (!VersionComparator.equal(software.version(), exactCpeVersion)) {
                return Optional.empty();
            }
            return Optional.of(new AssetMatch(
                    software.assetId(),
                    software.organizationId(),
                    software.hostname(),
                    cveId,
                    software.vendor(),
                    software.product(),
                    software.version(),
                    cpe,
                    MatchType.EXACT_VERSION_MATCH,
                    MatchConfidence.HIGH,
                    MatchExplanationFormatter.exactVersion(software, cveId, cpe)));
        }

        if (parsed != null && parsed.versionIsNotApplicable()) {
            return Optional.of(new AssetMatch(
                    software.assetId(),
                    software.organizationId(),
                    software.hostname(),
                    cveId,
                    software.vendor(),
                    software.product(),
                    software.version(),
                    cpe,
                    MatchType.CPE_MATCH,
                    MatchConfidence.HIGH,
                    MatchExplanationFormatter.cpeProduct(software, cveId, cpe)));
        }

        // Wildcard or missing CPE version and no range constraints: do not claim every version is vulnerable.
        return Optional.empty();
    }

    private static AssetMatch partial(String cveId, InstalledSoftware software, CpeConstraint cpe) {
        return new AssetMatch(
                software.assetId(),
                software.organizationId(),
                software.hostname(),
                cveId,
                software.vendor(),
                software.product(),
                software.version(),
                cpe,
                MatchType.PARTIAL_MATCH,
                MatchConfidence.MEDIUM,
                MatchExplanationFormatter.partial(software, cveId, cpe));
    }

    private static int rank(AssetMatch match) {
        int confidence = switch (match.confidence()) {
            case HIGH -> 300;
            case MEDIUM -> 200;
            case LOW -> 100;
        };
        int type = switch (match.matchType()) {
            case EXACT_VERSION_MATCH -> 4;
            case VERSION_RANGE_MATCH -> 3;
            case CPE_MATCH -> 2;
            case PARTIAL_MATCH -> 1;
        };
        return confidence + type;
    }
}
