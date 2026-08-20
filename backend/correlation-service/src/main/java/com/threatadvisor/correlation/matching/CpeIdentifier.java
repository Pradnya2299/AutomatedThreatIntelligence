package com.threatadvisor.correlation.matching;

import java.util.Arrays;
import java.util.Optional;

/**
 * CPE 2.3 (and a subset of CPE 2.2) parser.
 *
 * <p>Example: {@code cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*}
 *
 * <p>Limitations:
 * <ul>
 *   <li>Escaped colons in vendor/product names are not decoded.</li>
 *   <li>CPE 2.2 URI form is accepted when it has part, vendor, product, and optional version.</li>
 *   <li>{@code *} in the version component is a wildcard, not "every version is vulnerable".</li>
 *   <li>{@code -} is "not applicable" (common for OS products without a comparable version).</li>
 * </ul>
 */
public record CpeIdentifier(
        String raw,
        String part,
        String vendor,
        String product,
        String version,
        String update,
        String edition
) {

    public static Optional<CpeIdentifier> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.startsWith("cpe:2.3:")) {
            return parse23(value);
        }
        if (value.startsWith("cpe:/")) {
            return parse22(value);
        }
        return Optional.empty();
    }

    private static Optional<CpeIdentifier> parse23(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length < 6) {
            return Optional.empty();
        }
        return Optional.of(new CpeIdentifier(
                value,
                emptyToNull(parts[2]),
                emptyToNull(parts[3]),
                emptyToNull(parts[4]),
                emptyToNull(parts[5]),
                parts.length > 6 ? emptyToNull(parts[6]) : null,
                parts.length > 7 ? emptyToNull(parts[7]) : null
        ));
    }

    private static Optional<CpeIdentifier> parse22(String value) {
        String rest = value.substring("cpe:/".length());
        String[] parts = rest.split(":", -1);
        if (parts.length < 3) {
            return Optional.empty();
        }
        return Optional.of(new CpeIdentifier(
                value,
                emptyToNull(parts[0]),
                emptyToNull(parts[1]),
                emptyToNull(parts[2]),
                parts.length > 3 ? emptyToNull(parts[3]) : null,
                parts.length > 4 ? emptyToNull(parts[4]) : null,
                null
        ));
    }

    public boolean versionIsWildcard() {
        return version == null || "*".equals(version);
    }

    public boolean versionIsNotApplicable() {
        return "-".equals(version);
    }

    public boolean versionIsExact() {
        return VersionComparator.isComparable(version);
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    @Override
    public String toString() {
        return raw == null ? Arrays.toString(new String[] {vendor, product, version}) : raw;
    }
}
