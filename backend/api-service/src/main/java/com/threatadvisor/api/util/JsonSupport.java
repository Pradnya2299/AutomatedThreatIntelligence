package com.threatadvisor.api.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class JsonSupport {

    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    private JsonSupport() {
    }

    public static List<String> stringList(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, STRINGS);
        } catch (Exception ex) {
            return List.of();
        }
    }

    public static String textField(ObjectMapper mapper, String json, String field) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonNode node = mapper.readTree(json);
            if (node.hasNonNull(field)) {
                return node.get(field).asText();
            }
            if (node.isTextual()) {
                return node.asText();
            }
            return json;
        } catch (Exception ex) {
            return json;
        }
    }

    public static boolean demoModel(String modelName) {
        if (modelName == null) {
            return false;
        }
        String lower = modelName.toLowerCase();
        return lower.contains("demo") || lower.startsWith("mock");
    }
}
