package com.threatadvisor.ingestion.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * Canonical score/severity: CVSS v4, then v3.1, then v3.0, then v2. All versions are kept in cvss_metrics.
 */
public final class CvssExtractor {

    private CvssExtractor() {
    }

    public record CanonicalCvss(BigDecimal score, String vector, String severity, ObjectNode allVersions) {
    }

    public static CanonicalCvss extract(JsonNode payload) {
        ObjectNode all = JsonNodeFactory.instance.objectNode();
        JsonNode cvss = firstNonNull(payload.get("cvss"), payload.path("cve").path("metrics"));
        Optional<Metric> v4 = find(cvss, payload, "v4", "cvssMetricV40", "cvssMetricV4");
        Optional<Metric> v31 = find(cvss, payload, "v31", "cvssMetricV31", "v3.1");
        Optional<Metric> v30 = find(cvss, payload, "v30", "cvssMetricV30", "v3");
        Optional<Metric> v2 = find(cvss, payload, "v2", "cvssMetricV2", "cvssV2");

        v4.ifPresent(m -> all.set("v4", m.toJson()));
        v31.ifPresent(m -> all.set("v3.1", m.toJson()));
        v30.ifPresent(m -> all.set("v3.0", m.toJson()));
        v2.ifPresent(m -> all.set("v2", m.toJson()));

        Metric canonical = v4.orElse(v31.orElse(v30.orElse(v2.orElse(null))));
        if (canonical == null) {
            return new CanonicalCvss(null, null, "UNKNOWN", all);
        }
        return new CanonicalCvss(canonical.score, canonical.vector, canonical.severity, all);
    }

    private static Optional<Metric> find(JsonNode cvss, JsonNode payload, String simpleKey, String nvdKey, String alt) {
        if (cvss != null && cvss.has(simpleKey) && cvss.get(simpleKey).isObject()) {
            return Optional.ofNullable(fromObject(cvss.get(simpleKey)));
        }
        if (cvss != null && cvss.has(alt) && cvss.get(alt).isObject()) {
            return Optional.ofNullable(fromObject(cvss.get(alt)));
        }
        JsonNode nvd = payload.path("cve").path("metrics").path(nvdKey);
        if (nvd.isArray() && nvd.size() > 0) {
            return Optional.ofNullable(fromNvdMetric(nvd.get(0)));
        }
        if (nvd.isObject()) {
            return Optional.ofNullable(fromNvdMetric(nvd));
        }
        return Optional.empty();
    }

    private static Metric fromNvdMetric(JsonNode node) {
        JsonNode data = node.has("cvssData") ? node.get("cvssData") : node;
        Metric metric = fromObject(data);
        if (metric == null) {
            return null;
        }
        ObjectNode extra = metric.extra();
        copyText(data, extra, "attackVector", "attackComplexity", "privilegesRequired", "userInteraction",
                "scope", "confidentialityImpact", "integrityImpact", "availabilityImpact");
        copyText(node, extra, "exploitabilityScore", "impactScore");
        return metric;
    }

    private static void copyText(JsonNode source, ObjectNode target, String... fields) {
        for (String field : fields) {
            JsonNode value = source.get(field);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                if (value.isNumber()) {
                    target.put(field, value.decimalValue());
                } else {
                    target.put(field, value.asText());
                }
            }
        }
    }

    private static Metric fromObject(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        BigDecimal score = decimal(node, "baseScore", "score");
        String vector = text(node, "vectorString", "vector");
        String severity = text(node, "baseSeverity", "severity");
        if (severity != null) {
            severity = severity.trim().toUpperCase(Locale.ROOT);
        } else if (score != null) {
            severity = severityFromScore(score);
        }
        if (score == null && vector == null && severity == null) {
            return null;
        }
        return new Metric(score, vector, severity == null ? "UNKNOWN" : severity, JsonNodeFactory.instance.objectNode());
    }

    private static String severityFromScore(BigDecimal score) {
        double value = score.doubleValue();
        if (value >= 9.0) {
            return "CRITICAL";
        }
        if (value >= 7.0) {
            return "HIGH";
        }
        if (value >= 4.0) {
            return "MEDIUM";
        }
        if (value > 0) {
            return "LOW";
        }
        return "NONE";
    }

    private static BigDecimal decimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isNumber()) {
                return value.decimalValue();
            }
            if (value != null && value.isTextual()) {
                try {
                    return new BigDecimal(value.asText());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static JsonNode firstNonNull(JsonNode a, JsonNode b) {
        if (a != null && !a.isMissingNode() && !a.isNull()) {
            return a;
        }
        return b;
    }

    private record Metric(BigDecimal score, String vector, String severity, ObjectNode extra) {
        ObjectNode toJson() {
            ObjectNode node = extra == null ? JsonNodeFactory.instance.objectNode() : extra.deepCopy();
            if (score != null) {
                node.put("score", score);
            }
            if (vector != null) {
                node.put("vector", vector);
            }
            if (severity != null) {
                node.put("severity", severity);
            }
            return node;
        }
    }
}
