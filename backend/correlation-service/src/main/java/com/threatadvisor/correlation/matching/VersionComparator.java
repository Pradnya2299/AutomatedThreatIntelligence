package com.threatadvisor.correlation.matching;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Numeric-aware version comparison.
 *
 * <p>Uses Maven {@link ComparableVersion} (the algorithm Maven uses for artifact versions).
 * It treats dotted numeric segments as integers, so {@code 2.4.10 > 2.4.9}.
 * Do not use {@link String#compareTo(String)} for software versions.
 *
 * <p>Why this library: it is small, widely used, and already handles common patch/qualifier
 * forms ({@code 17.0.8}, {@code 21.0.1}, {@code 1.2.3-beta}) without a custom parser.
 */
public final class VersionComparator {

    private VersionComparator() {
    }

    public static ComparableVersion parse(String version) {
        if (version == null || version.isBlank() || "*".equals(version.trim()) || "-".equals(version.trim())) {
            throw new IllegalArgumentException("Version is missing or a CPE wildcard: " + version);
        }
        return new ComparableVersion(version.trim());
    }

    public static int compare(String left, String right) {
        return parse(left).compareTo(parse(right));
    }

    public static boolean equal(String left, String right) {
        return compare(left, right) == 0;
    }

    public static boolean isComparable(String version) {
        return version != null && !version.isBlank() && !"*".equals(version.trim()) && !"-".equals(version.trim());
    }
}
