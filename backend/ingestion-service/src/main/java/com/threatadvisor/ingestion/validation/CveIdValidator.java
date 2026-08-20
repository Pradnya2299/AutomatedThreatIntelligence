package com.threatadvisor.ingestion.validation;

import com.threatadvisor.ingestion.exception.IngestionException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

/**
 * CVE IDs are MITRE identifiers of the form CVE-YYYY-N+ (4+ digits after the year).
 * Invalid values are rejected; they are never rewritten.
 */
public final class CveIdValidator {

    private static final Pattern CVE_ID = Pattern.compile("^CVE-\\d{4}-\\d{4,}$");

    private CveIdValidator() {
    }

    public static boolean isValid(String cveId) {
        return cveId != null && CVE_ID.matcher(cveId).matches();
    }

    public static String requireValid(String cveId) {
        if (cveId == null || cveId.isBlank()) {
            throw new IngestionException(HttpStatus.BAD_REQUEST, "CVE_ID_MISSING", "CVE identifier is required");
        }
        String trimmed = cveId.trim();
        if (!isValid(trimmed)) {
            throw new IngestionException(
                    HttpStatus.BAD_REQUEST,
                    "CVE_ID_INVALID",
                    "CVE identifier is invalid. Expected CVE-YYYY-NNNN (4+ digits after the year) without rewriting."
            );
        }
        return trimmed;
    }
}
