package com.threatadvisor.ai.coderemediation.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class NpmPackagePatcher {

    private NpmPackagePatcher() {
    }

    public static String currentVersion(ObjectMapper mapper, String packageJson, String packageName) {
        try {
            JsonNode root = mapper.readTree(packageJson);
            JsonNode deps = root.path("dependencies").path(packageName);
            if (deps.isMissingNode() || deps.isNull()) {
                deps = root.path("devDependencies").path(packageName);
            }
            return deps.isMissingNode() ? null : deps.asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public static String upgrade(ObjectMapper mapper, String packageJson, String packageName, String newVersion) {
        try {
            JsonNode root = mapper.readTree(packageJson);
            if (!(root instanceof ObjectNode obj)) {
                return packageJson;
            }
            for (String key : new String[] {"dependencies", "devDependencies"}) {
                JsonNode node = obj.get(key);
                if (node instanceof ObjectNode deps && deps.has(packageName)) {
                    deps.put(packageName, newVersion);
                }
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        } catch (Exception ex) {
            return packageJson;
        }
    }
}
