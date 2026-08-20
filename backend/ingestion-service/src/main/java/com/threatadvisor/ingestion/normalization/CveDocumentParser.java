package com.threatadvisor.ingestion.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ingestion.exception.IngestionException;
import com.threatadvisor.ingestion.validation.CveIdValidator;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CveDocumentParser {

    private CveDocumentParser() {
    }

    public static NormalizedVulnerability parse(JsonNode payload, String defaultSource) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new IngestionException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_PAYLOAD", "CVE payload must be a JSON object");
        }
        String cveId = CveIdValidator.requireValid(extractCveId(payload));
        String description = DescriptionExtractor.extractEnglishOrFirst(payload);
        if (description == null || description.isBlank()) {
            throw new IngestionException(HttpStatus.BAD_REQUEST, "DESCRIPTION_MISSING", "An English or fallback description is required");
        }
        CvssExtractor.CanonicalCvss cvss = CvssExtractor.extract(payload);
        List<CpeExtractor.ExtractedCpe> cpes = CpeExtractor.extract(payload);
        List<String> cwes = extractCwes(payload);
        String primaryCwe = cwes.isEmpty() ? null : cwes.getFirst();
        List<String> vendors = stringList(payload, "vendors", "affectedVendors");
        List<String> products = stringList(payload, "products", "affectedProducts");
        if (vendors.isEmpty()) {
            vendors = cpes.stream().map(CpeExtractor.ExtractedCpe::vendor).filter(v -> v != null).distinct().toList();
        }
        if (products.isEmpty()) {
            products = cpes.stream().map(CpeExtractor.ExtractedCpe::product).filter(v -> v != null).distinct().toList();
        }
        String source = firstText(payload, "source");
        if (source == null || source.isBlank()) {
            source = defaultSource;
        }
        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        JsonNode otherDescriptions = payload.get("descriptions");
        if (otherDescriptions != null) {
            metadata.set("descriptions", otherDescriptions);
        }
        if (payload.has("cve") && payload.get("cve").has("descriptions")) {
            metadata.set("nvdDescriptions", payload.get("cve").get("descriptions"));
        }
        return new NormalizedVulnerability(
                cveId,
                description,
                parseInstant(firstText(payload, "published", "publishedAt", "publishedDate")),
                parseInstant(firstText(payload, "lastModified", "modifiedAt", "lastModifiedDate")),
                cvss.score(),
                cvss.vector(),
                cvss.severity(),
                primaryCwe,
                cwes,
                vendors,
                products,
                bool(payload, false, "exploitAvailable", "exploit_available"),
                bool(payload, false, "activelyExploited", "actively_exploited"),
                source.toLowerCase(Locale.ROOT),
                firstText(payload, "sourceUrl", "source_url"),
                cvss.allVersions(),
                metadata,
                cpes,
                stringList(payload, "references", "referenceUrls"),
                payload
        );
    }

    public static String extractCveId(JsonNode payload) {
        String id = firstText(payload, "cveId", "cve_id", "id");
        if (id != null) {
            return id;
        }
        JsonNode cve = payload.get("cve");
        if (cve != null) {
            return firstText(cve, "id", "CVE_data_meta");
        }
        return null;
    }

    private static List<String> extractCwes(JsonNode payload) {
        List<String> cwes = new ArrayList<>(stringList(payload, "cwes", "cwe"));
        JsonNode weaknesses = payload.path("cve").path("weaknesses");
        if (weaknesses.isArray()) {
            for (JsonNode weakness : weaknesses) {
                JsonNode descriptions = weakness.get("description");
                if (descriptions != null && descriptions.isArray()) {
                    for (JsonNode item : descriptions) {
                        String value = item.path("value").asText(null);
                        if (value != null && value.startsWith("CWE-")) {
                            cwes.add(value);
                        }
                    }
                }
            }
        }
        return cwes.stream().distinct().toList();
    }

    private static List<String> stringList(JsonNode payload, String... fields) {
        List<String> values = new ArrayList<>();
        for (String field : fields) {
            JsonNode node = payload.get(field);
            if (node == null) {
                continue;
            }
            if (node.isTextual() && !node.asText().isBlank()) {
                values.add(node.asText());
            } else if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isTextual() && !item.asText().isBlank()) {
                        values.add(item.asText());
                    } else if (item.isObject() && item.has("url")) {
                        values.add(item.get("url").asText());
                    }
                }
            }
        }
        JsonNode nvdRefs = payload.path("cve").path("references");
        if (nvdRefs.isArray() && (fields[0].equals("references") || fields[0].equals("referenceUrls"))) {
            for (JsonNode item : nvdRefs) {
                if (item.has("url")) {
                    values.add(item.get("url").asText());
                }
            }
        }
        return values.stream().distinct().toList();
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
            if (value != null && value.isObject() && value.has("ID")) {
                return value.get("ID").asText();
            }
        }
        if (node.has("cve")) {
            JsonNode cve = node.get("cve");
            for (String field : fields) {
                JsonNode value = cve.get(field);
                if (value != null && value.isTextual() && !value.asText().isBlank()) {
                    return value.asText().trim();
                }
            }
        }
        return null;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                return Instant.parse(value + "Z");
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static boolean bool(JsonNode payload, boolean defaultValue, String... fields) {
        for (String field : fields) {
            JsonNode value = payload.get(field);
            if (value != null && value.isBoolean()) {
                return value.booleanValue();
            }
        }
        return defaultValue;
    }
}
