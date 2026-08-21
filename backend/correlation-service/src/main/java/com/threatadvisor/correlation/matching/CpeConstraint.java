package com.threatadvisor.correlation.matching;

public record CpeConstraint(
        String cpe,
        String vendor,
        String product,
        String versionStartIncluding,
        String versionStartExcluding,
        String versionEndIncluding,
        String versionEndExcluding
) {

    public VersionRange range() {
        return new VersionRange(versionStartIncluding, versionStartExcluding, versionEndIncluding, versionEndExcluding);
    }

    public String vendorOrParsed() {
        return firstNonBlank(vendor, CpeIdentifier.parse(cpe).map(CpeIdentifier::vendor).orElse(null));
    }

    public String productOrParsed() {
        return firstNonBlank(product, CpeIdentifier.parse(cpe).map(CpeIdentifier::product).orElse(null));
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
