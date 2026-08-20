package com.threatadvisor.correlation.matching;

import java.util.Locale;

public final class MatchExplanationFormatter {

    private MatchExplanationFormatter() {
    }

    public static String exactVersion(InstalledSoftware software, String cveId, CpeConstraint cpe) {
        return "Asset %s is affected by %s because it runs %s %s, which matches the vulnerable CPE %s."
                .formatted(
                        software.hostname(),
                        cveId,
                        displayName(software.vendor(), software.product()),
                        software.version(),
                        displayCpe(cpe));
    }

    public static String versionRange(InstalledSoftware software, String cveId, CpeConstraint cpe) {
        return "Asset %s runs %s %s, which falls within the vulnerable range %s for %s."
                .formatted(
                        software.hostname(),
                        displayName(software.vendor(), software.product()),
                        software.version(),
                        cpe.range().describe(),
                        cveId);
    }

    public static String cpeProduct(InstalledSoftware software, String cveId, CpeConstraint cpe) {
        return "Asset %s is affected by %s because it runs %s, which matches CPE %s (version not applicable)."
                .formatted(
                        software.hostname(),
                        cveId,
                        displayName(software.vendor(), software.product()),
                        displayCpe(cpe));
    }

    public static String partial(InstalledSoftware software, String cveId, CpeConstraint cpe) {
        return "Asset %s has %s installed without a comparable version, so %s is only a partial match against CPE %s."
                .formatted(
                        software.hostname(),
                        displayName(software.vendor(), software.product()),
                        cveId,
                        displayCpe(cpe));
    }

    public static String displayName(String vendor, String product) {
        String v = title(vendor);
        String p = title(product == null ? "" : product.replace('_', ' ').replace('-', ' '));
        if (v.isBlank()) {
            return p;
        }
        if (p.isBlank() || p.toLowerCase(Locale.ROOT).startsWith(v.toLowerCase(Locale.ROOT))) {
            return p.isBlank() ? v : p;
        }
        return (v + " " + p).trim();
    }

    private static String displayCpe(CpeConstraint cpe) {
        if (cpe.cpe() != null && !cpe.cpe().isBlank()) {
            return cpe.cpe();
        }
        return "vendor=%s product=%s".formatted(cpe.vendorOrParsed(), cpe.productOrParsed());
    }

    private static String title(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] words = value.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            String lower = word.toLowerCase(Locale.ROOT);
            if ("http".equals(lower) || "https".equals(lower) || "iis".equals(lower) || "jdk".equals(lower)) {
                out.append(lower.toUpperCase(Locale.ROOT));
            } else {
                out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return out.toString();
    }
}
