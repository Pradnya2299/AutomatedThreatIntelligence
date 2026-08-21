package com.threatadvisor.ai.coderemediation.patch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DockerfilePatcher {

    private DockerfilePatcher() {
    }

    public static String currentFrom(String dockerfile) {
        if (dockerfile == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?m)^FROM\\s+(\\S+)").matcher(dockerfile);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String replaceFrom(String dockerfile, String newImage) {
        if (dockerfile == null) {
            return null;
        }
        return dockerfile.replaceFirst("(?m)^FROM\\s+\\S+", "FROM " + newImage);
    }
}
