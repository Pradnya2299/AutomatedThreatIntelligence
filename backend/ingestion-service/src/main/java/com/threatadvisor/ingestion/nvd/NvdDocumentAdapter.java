package com.threatadvisor.ingestion.nvd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an NVD 2.0 API envelope into documents {@link com.threatadvisor.ingestion.normalization.CveDocumentParser} understands.
 */
public final class NvdDocumentAdapter {

    private NvdDocumentAdapter() {
    }

    public static List<JsonNode> extractCveDocuments(JsonNode nvdResponse) {
        List<JsonNode> documents = new ArrayList<>();
        if (nvdResponse == null || nvdResponse.isMissingNode() || nvdResponse.isNull()) {
            return documents;
        }
        JsonNode vulns = nvdResponse.get("vulnerabilities");
        if (vulns != null && vulns.isArray()) {
            for (JsonNode item : vulns) {
                JsonNode cve = item.path("cve");
                if (!cve.isMissingNode() && cve.isObject()) {
                    documents.add(wrap(cve, nvdResponse));
                }
            }
            return documents;
        }
        if (nvdResponse.has("cve") || nvdResponse.has("cveId")) {
            documents.add(nvdResponse);
        }
        return documents;
    }

    private static JsonNode wrap(JsonNode cve, JsonNode envelope) {
        ObjectNode document = JsonNodeFactory.instance.objectNode();
        document.set("cve", cve.deepCopy());
        document.put("source", "nvd");
        document.put("sourceUrl", "https://nvd.nist.gov/vuln/detail/" + cve.path("id").asText(""));
        if (cve.hasNonNull("sourceIdentifier")) {
            document.put("sourceIdentifier", cve.get("sourceIdentifier").asText());
        }
        if (cve.hasNonNull("published")) {
            document.put("published", cve.get("published").asText());
        }
        if (cve.hasNonNull("lastModified")) {
            document.put("lastModified", cve.get("lastModified").asText());
        }
        ArrayNode refs = JsonNodeFactory.instance.arrayNode();
        JsonNode nvdRefs = cve.path("references");
        if (nvdRefs.isArray()) {
            for (JsonNode ref : nvdRefs) {
                if (ref.has("url")) {
                    refs.add(ref.get("url").asText());
                }
            }
        }
        document.set("references", refs);
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.put("nvdResultsPerPage", envelope.path("resultsPerPage").asInt(0));
        document.set("nvdEnvelope", meta);
        return document;
    }
}
