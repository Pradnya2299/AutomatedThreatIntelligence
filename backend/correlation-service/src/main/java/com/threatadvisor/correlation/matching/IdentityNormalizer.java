package com.threatadvisor.correlation.matching;

import java.util.Locale;

/**
 * Vendor/product identity keys used for deterministic comparison.
 *
 * <p>Rules:
 * <ol>
 *   <li>Trim whitespace.</li>
 *   <li>Lowercase using {@link Locale#ROOT}.</li>
 *   <li>Remove every character that is not ASCII letter or digit.
 *       {@code http_server}, {@code http-server}, and {@code HTTP Server} become {@code httpserver}.</li>
 * </ol>
 *
 * Version strings are not passed through this normalizer; see {@link VersionComparator}.
 */
public final class IdentityNormalizer {

    private IdentityNormalizer() {
    }

    public static String key(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static boolean keysEqual(String left, String right) {
        String a = key(left);
        String b = key(right);
        return !a.isEmpty() && a.equals(b);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank() || "*".equals(value.trim()) || "-".equals(value.trim());
    }
}
