package com.threatadvisor.correlation.matching;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationMatchingUnitTest {

    private final CorrelationEngine engine = new CorrelationEngine();

    @Test
    void parsesCpe23() {
        CpeIdentifier cpe = CpeIdentifier.parse("cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*").orElseThrow();
        assertEquals("a", cpe.part());
        assertEquals("apache", cpe.vendor());
        assertEquals("http_server", cpe.product());
        assertEquals("2.4.49", cpe.version());
        assertTrue(cpe.versionIsExact());
        assertFalse(cpe.versionIsWildcard());
    }

    @Test
    void parsesWildcardAndNaVersions() {
        assertTrue(CpeIdentifier.parse("cpe:2.3:a:apache:log4j:*:*:*:*:*:*:*:*").orElseThrow().versionIsWildcard());
        assertTrue(CpeIdentifier.parse("cpe:2.3:o:microsoft:windows_11:-:*:*:*:*:*:*:*").orElseThrow().versionIsNotApplicable());
    }

    @Test
    void normalizesVendorAndProduct() {
        assertEquals("apache", IdentityNormalizer.key("Apache"));
        assertEquals("httpserver", IdentityNormalizer.key("http_server"));
        assertEquals("httpserver", IdentityNormalizer.key("HTTP-Server"));
        assertTrue(IdentityNormalizer.keysEqual("http_server", "http-server"));
        assertFalse(IdentityNormalizer.keysEqual("apache", "nginx"));
    }

    @Test
    void comparesVersionsNumerically() {
        assertTrue(VersionComparator.compare("2.4.9", "2.4.10") < 0);
        assertTrue(VersionComparator.compare("2.4.10", "2.4.9") > 0);
        assertTrue(VersionComparator.compare("17.0.9", "17.0.8") > 0);
        assertTrue(VersionComparator.compare("21.0.1", "21.0.0") > 0);
        assertTrue(VersionComparator.equal("2.4.49", "2.4.49"));
        assertTrue("2.4.10".compareTo("2.4.9") < 0, "sanity: lexicographic order is the WRONG order");
        assertTrue(VersionComparator.compare("2.4.10", "2.4.9") > 0);
    }

    @Test
    void evaluatesInclusiveExclusiveRanges() {
        VersionRange openssl = new VersionRange("3.0.0", null, null, "3.0.7");
        assertTrue(openssl.contains("3.0.2"));
        assertTrue(openssl.contains("3.0.0"));
        assertFalse(openssl.contains("3.0.7"));
        assertFalse(openssl.contains("3.0.13"));

        VersionRange exclusiveStart = new VersionRange(null, "2.4.0", "2.4.49", null);
        assertFalse(exclusiveStart.contains("2.4.0"));
        assertTrue(exclusiveStart.contains("2.4.1"));
        assertTrue(exclusiveStart.contains("2.4.49"));
        assertFalse(exclusiveStart.contains("2.4.50"));
    }

    @Test
    void wildcardWithoutRangeDoesNotMatch() {
        InstalledSoftware sw = software("web-01", "apache", "http_server", "2.4.49");
        CpeConstraint wildcard = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*",
                "apache", "http_server", null, null, null, null);
        assertTrue(engine.match("CVE-2025-1234", sw, wildcard).isEmpty());
    }

    @Test
    void exactVersionMatchCreatesFinding() {
        InstalledSoftware sw = software("web-prod-01", "apache", "http_server", "2.4.49");
        CpeConstraint exact = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*",
                "apache", "http_server", null, null, null, null);
        AssetMatch match = engine.match("CVE-2025-1234", sw, exact).orElseThrow();
        assertEquals(MatchType.EXACT_VERSION_MATCH, match.matchType());
        assertEquals(MatchConfidence.HIGH, match.confidence());
        assertTrue(match.explanation().contains("web-prod-01"));
        assertTrue(match.explanation().contains("CVE-2025-1234"));
        assertTrue(match.explanation().contains("2.4.49"));
    }

    @Test
    void nonMatchingVendorProducesNoFinding() {
        InstalledSoftware nginx = software("edge-01", "nginx", "nginx", "1.24.0");
        CpeConstraint apache = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*",
                "apache", "http_server", null, null, null, null);
        assertTrue(engine.match("CVE-2025-1234", nginx, apache).isEmpty());
    }

    @Test
    void nonMatchingProductProducesNoFinding() {
        InstalledSoftware kafka = software("kafka-01", "apache", "kafka", "3.6.1");
        CpeConstraint httpd = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*",
                "apache", "http_server", null, null, null, null);
        assertTrue(engine.match("CVE-2023-25690", kafka, httpd).isEmpty());
    }

    @Test
    void nonVulnerableVersionProducesNoFinding() {
        InstalledSoftware patched = software("web-02", "apache", "http_server", "2.4.51");
        CpeConstraint exact = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*",
                "apache", "http_server", null, null, null, null);
        assertTrue(engine.match("CVE-2025-1234", patched, exact).isEmpty());
    }

    @Test
    void vulnerableRangeMatchAndExplanation() {
        InstalledSoftware sw = software("api-prod-02", "openssl", "openssl", "3.0.7");
        CpeConstraint range = new CpeConstraint(
                "cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*",
                "openssl", "openssl", "3.0.0", null, null, "3.0.8");
        AssetMatch match = engine.match("CVE-2022-3602", sw, range).orElseThrow();
        assertEquals(MatchType.VERSION_RANGE_MATCH, match.matchType());
        assertEquals(MatchConfidence.HIGH, match.confidence());
        assertTrue(match.explanation().contains("api-prod-02"));
        assertTrue(match.explanation().contains(">= 3.0.0"));
        assertTrue(match.explanation().contains("< 3.0.8"));
    }

    @Test
    void rangeBelowAndAboveAreExcluded() {
        CpeConstraint range = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*",
                "apache", "http_server", "2.4.0", null, null, "2.4.56");
        assertTrue(engine.match("CVE-2023-25690", software("old", "apache", "http_server", "2.3.9"), range).isEmpty());
        assertTrue(engine.match("CVE-2023-25690", software("in", "apache", "http_server", "2.4.49"), range).isPresent());
        assertTrue(engine.match("CVE-2023-25690", software("new", "apache", "http_server", "2.4.57"), range).isEmpty());
    }

    @Test
    void naCpeVersionMatchesProductWithoutClaimingEveryVersion() {
        InstalledSoftware win = software("nw-dev-win-01", "microsoft", "windows", "11");
        CpeConstraint na = new CpeConstraint(
                "cpe:2.3:o:microsoft:windows_11:-:*:*:*:*:*:*:*",
                "microsoft", "windows", null, null, null, null);
        AssetMatch match = engine.match("CVE-2023-36025", win, na).orElseThrow();
        assertEquals(MatchType.CPE_MATCH, match.matchType());
        assertEquals(MatchConfidence.HIGH, match.confidence());
    }

    @Test
    void missingInstalledVersionIsMediumPartial() {
        InstalledSoftware unknown = software("host-1", "apache", "http_server", null);
        CpeConstraint exact = new CpeConstraint(
                "cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*",
                "apache", "http_server", null, null, null, null);
        AssetMatch match = engine.match("CVE-2025-1234", unknown, exact).orElseThrow();
        assertEquals(MatchType.PARTIAL_MATCH, match.matchType());
        assertEquals(MatchConfidence.MEDIUM, match.confidence());
    }

    @Test
    void evaluateDedupesPerAsset() {
        UUID asset = UUID.randomUUID();
        InstalledSoftware one = new InstalledSoftware(asset, UUID.randomUUID(), "web-01", "apache", "http_server", "2.4.49", null);
        List<AssetMatch> matches = engine.evaluate(
                "CVE-2025-1234",
                List.of(new CpeConstraint("cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*", "apache", "http_server", null, null, null, null)),
                List.of(one, one));
        assertEquals(1, matches.size());
        assertEquals(MatchConfidence.HIGH, matches.get(0).confidence());
    }

    private static InstalledSoftware software(String host, String vendor, String product, String version) {
        return new InstalledSoftware(UUID.randomUUID(), UUID.randomUUID(), host, vendor, product, version, null);
    }
}
