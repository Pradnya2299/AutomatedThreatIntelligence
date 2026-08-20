package com.threatadvisor.ingestion.normalization;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts CPE rows only. Does not compare versions or match assets.
 */
public final class CpeExtractor {

    private CpeExtractor() {
    }

    public record ExtractedCpe(
            String cpe,
            String vendor,
            String product,
            String versionStartIncluding,
            String versionStartExcluding,
            String versionEndIncluding,
            String versionEndExcluding,
            String operatingSystem,
            String architecture
    ) {
    }

    public static List<ExtractedCpe> extract(JsonNode payload) {
        Set<String> seen = new LinkedHashSet<>();
        List<ExtractedCpe> out = new ArrayList<>();
        collectArray(payload.get("cpes"), seen, out);
        collectArray(payload.path("cve").get("cpes"), seen, out);
        walkNvd(payload.path("cve").path("configurations"), seen, out);
        return List.copyOf(out);
    }

    private static void collectArray(JsonNode array, Set<String> seen, List<ExtractedCpe> out) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonNode item : array) {
            add(fromNode(item), seen, out);
        }
    }

    private static void walkNvd(JsonNode configurations, Set<String> seen, List<ExtractedCpe> out) {
        if (configurations == null || configurations.isMissingNode()) {
            return;
        }
        if (configurations.isArray()) {
            for (JsonNode config : configurations) {
                walkNvd(config, seen, out);
            }
            return;
        }
        JsonNode nodes = configurations.get("nodes");
        if (nodes != null && nodes.isArray()) {
            for (JsonNode node : nodes) {
                JsonNode matches = node.get("cpeMatch");
                if (matches != null && matches.isArray()) {
                    for (JsonNode match : matches) {
                        add(fromNvdMatch(match), seen, out);
                    }
                }
                walkNvd(node.get("children"), seen, out);
            }
        }
    }

    private static ExtractedCpe fromNvdMatch(JsonNode match) {
        String cpe = text(match, "criteria", "cpe23Uri", "cpe");
        return new ExtractedCpe(
                cpe,
                vendorFromCpe(cpe),
                productFromCpe(cpe),
                text(match, "versionStartIncluding"),
                text(match, "versionStartExcluding"),
                text(match, "versionEndIncluding"),
                text(match, "versionEndExcluding"),
                null,
                null
        );
    }

    private static ExtractedCpe fromNode(JsonNode item) {
        if (item == null || item.isNull()) {
            return null;
        }
        if (item.isTextual()) {
            String cpe = item.asText();
            return new ExtractedCpe(cpe, vendorFromCpe(cpe), productFromCpe(cpe), null, null, null, null, null, null);
        }
        String cpe = text(item, "cpe", "criteria", "cpe23Uri");
        String vendor = text(item, "vendor");
        String product = text(item, "product");
        if (vendor == null) {
            vendor = vendorFromCpe(cpe);
        }
        if (product == null) {
            product = productFromCpe(cpe);
        }
        return new ExtractedCpe(
                cpe,
                vendor,
                product,
                text(item, "versionStartIncluding", "version_start_including"),
                text(item, "versionStartExcluding", "version_start_excluding"),
                text(item, "versionEndIncluding", "version_end_including"),
                text(item, "versionEndExcluding", "version_end_excluding"),
                text(item, "operatingSystem", "operating_system"),
                text(item, "architecture")
        );
    }

    private static void add(ExtractedCpe cpe, Set<String> seen, List<ExtractedCpe> out) {
        if (cpe == null || cpe.cpe() == null || cpe.cpe().isBlank()) {
            return;
        }
        if (seen.add(cpe.cpe())) {
            out.add(cpe);
        }
    }

    static String vendorFromCpe(String cpe) {
        String[] parts = splitCpe(cpe);
        return parts.length > 3 ? emptyToNull(parts[3]) : null;
    }

    static String productFromCpe(String cpe) {
        String[] parts = splitCpe(cpe);
        return parts.length > 4 ? emptyToNull(parts[4]) : null;
    }

    private static String[] splitCpe(String cpe) {
        if (cpe == null) {
            return new String[0];
        }
        return cpe.split(":");
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank() || "*".equals(value) || "-".equals(value)) {
            return null;
        }
        return value;
    }

    private static String text(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }
}
