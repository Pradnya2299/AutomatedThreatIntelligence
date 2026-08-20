package com.threatadvisor.correlation.matching;

/**
 * Inclusive/exclusive version range from {@code vulnerability_cpe} columns.
 *
 * <p>An installed version is in range when all provided bounds hold:
 * {@code startIncluding <= v}, {@code startExcluding < v}, {@code v <= endIncluding}, {@code v < endExcluding}.
 */
public record VersionRange(
        String startIncluding,
        String startExcluding,
        String endIncluding,
        String endExcluding
) {

    public boolean hasBound() {
        return VersionComparator.isComparable(startIncluding)
                || VersionComparator.isComparable(startExcluding)
                || VersionComparator.isComparable(endIncluding)
                || VersionComparator.isComparable(endExcluding);
    }

    public boolean contains(String installedVersion) {
        if (!VersionComparator.isComparable(installedVersion) || !hasBound()) {
            return false;
        }
        if (VersionComparator.isComparable(startIncluding)
                && VersionComparator.compare(installedVersion, startIncluding) < 0) {
            return false;
        }
        if (VersionComparator.isComparable(startExcluding)
                && VersionComparator.compare(installedVersion, startExcluding) <= 0) {
            return false;
        }
        if (VersionComparator.isComparable(endIncluding)
                && VersionComparator.compare(installedVersion, endIncluding) > 0) {
            return false;
        }
        if (VersionComparator.isComparable(endExcluding)
                && VersionComparator.compare(installedVersion, endExcluding) >= 0) {
            return false;
        }
        return true;
    }

    public String describe() {
        StringBuilder text = new StringBuilder();
        if (VersionComparator.isComparable(startIncluding)) {
            append(text, ">= " + startIncluding);
        }
        if (VersionComparator.isComparable(startExcluding)) {
            append(text, "> " + startExcluding);
        }
        if (VersionComparator.isComparable(endIncluding)) {
            append(text, "<= " + endIncluding);
        }
        if (VersionComparator.isComparable(endExcluding)) {
            append(text, "< " + endExcluding);
        }
        return text.isEmpty() ? "unspecified" : text.toString();
    }

    private static void append(StringBuilder text, String clause) {
        if (!text.isEmpty()) {
            text.append(" and ");
        }
        text.append(clause);
    }
}
