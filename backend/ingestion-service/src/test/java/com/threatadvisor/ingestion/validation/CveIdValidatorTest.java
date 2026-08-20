package com.threatadvisor.ingestion.validation;

import com.threatadvisor.ingestion.exception.IngestionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CveIdValidatorTest {

    @Test
    void acceptsFourOrMoreDigitsAfterYear() {
        assertTrue(CveIdValidator.isValid("CVE-2024-1234"));
        assertTrue(CveIdValidator.isValid("CVE-2024-1234567"));
    }

    @Test
    void rejectsMalformedIdsWithoutRewriting() {
        assertFalse(CveIdValidator.isValid("cve-2024-1234"));
        assertFalse(CveIdValidator.isValid("CVE-24-1234"));
        assertFalse(CveIdValidator.isValid("CVE-2024-123"));
        IngestionException ex = assertThrows(IngestionException.class, () -> CveIdValidator.requireValid("cve-2024-1234"));
        assertEquals("CVE_ID_INVALID", ex.getCode());
    }

    @Test
    void rejectsMissingId() {
        IngestionException ex = assertThrows(IngestionException.class, () -> CveIdValidator.requireValid("  "));
        assertEquals("CVE_ID_MISSING", ex.getCode());
    }
}
