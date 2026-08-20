package com.threatadvisor.ai.coderemediation.patch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenDependencyPatcher {

    private MavenDependencyPatcher() {
    }

    public static boolean containsArtifact(String pom, String artifactId) {
        return pom != null && artifactId != null && pom.contains("<artifactId>" + artifactId + "</artifactId>");
    }

    public static String currentVersion(String pom, String artifactId) {
        if (pom == null || artifactId == null) {
            return null;
        }
        Pattern block = Pattern.compile(
                "<dependency>[\\s\\S]*?<artifactId>" + Pattern.quote(artifactId) + "</artifactId>[\\s\\S]*?</dependency>",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = block.matcher(pom);
        if (!matcher.find()) {
            return null;
        }
        Matcher version = Pattern.compile("<version>([^<]+)</version>").matcher(matcher.group());
        return version.find() ? version.group(1).trim() : null;
    }

    public static String upgrade(String pom, String artifactId, String newVersion) {
        if (pom == null) {
            return null;
        }
        Pattern block = Pattern.compile(
                "(<dependency>[\\s\\S]*?<artifactId>" + Pattern.quote(artifactId) + "</artifactId>[\\s\\S]*?<version>)([^<]+)(</version>[\\s\\S]*?</dependency>)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = block.matcher(pom);
        if (!matcher.find()) {
            return pom;
        }
        return pom.substring(0, matcher.start())
                + matcher.group(1)
                + newVersion
                + matcher.group(3)
                + pom.substring(matcher.end());
    }
}
