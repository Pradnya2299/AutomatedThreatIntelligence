package com.threatadvisor.ingestion.normalization;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prefers English. Does not translate or call an LLM.
 */
public final class DescriptionExtractor {

    private DescriptionExtractor() {
    }

    public static String extractEnglishOrFirst(JsonNode payload) {
        List<LangText> texts = new ArrayList<>();
        collect(payload.get("descriptions"), texts);
        JsonNode cve = payload.get("cve");
        if (cve != null) {
            collect(cve.get("descriptions"), texts);
        }
        if (payload.hasNonNull("description") && payload.get("description").isTextual()) {
            texts.add(new LangText("en", payload.get("description").asText()));
        }
        if (cve != null && cve.hasNonNull("description") && cve.get("description").isTextual()) {
            texts.add(new LangText("en", cve.get("description").asText()));
        }

        String english = null;
        String first = null;
        for (LangText text : texts) {
            if (text.value() == null || text.value().isBlank()) {
                continue;
            }
            if (first == null) {
                first = text.value().trim();
            }
            String lang = text.lang() == null ? "" : text.lang().toLowerCase(Locale.ROOT);
            if (english == null && (lang.equals("en") || lang.startsWith("en-") || lang.equals("eng"))) {
                english = text.value().trim();
            }
        }
        if (english != null) {
            return english;
        }
        return first;
    }

    private static void collect(JsonNode descriptions, List<LangText> out) {
        if (descriptions == null || !descriptions.isArray()) {
            return;
        }
        for (JsonNode item : descriptions) {
            if (item == null) {
                continue;
            }
            if (item.isTextual()) {
                out.add(new LangText("und", item.asText()));
                continue;
            }
            String lang = textOrNull(item, "lang");
            String value = textOrNull(item, "value");
            if (value == null) {
                value = textOrNull(item, "description");
            }
            out.add(new LangText(lang, value));
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private record LangText(String lang, String value) {
    }
}
