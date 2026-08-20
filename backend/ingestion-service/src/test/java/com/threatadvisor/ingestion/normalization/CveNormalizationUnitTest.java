package com.threatadvisor.ingestion.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ingestion.exception.IngestionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CveNormalizationUnitTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void prefersEnglishDescription() throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.putArray("descriptions")
                .add(mapper.createObjectNode().put("lang", "es").put("value", "hola"))
                .add(mapper.createObjectNode().put("lang", "en").put("value", "english text"));
        assertEquals("english text", DescriptionExtractor.extractEnglishOrFirst(payload));
    }

    @Test
    void canonicalCvssPrefersV31OverV2() throws Exception {
        var payload = mapper.readTree("""
                {"cvss":{"v31":{"score":9.8,"severity":"CRITICAL","vector":"CVSS:3.1/AV:N"},
                         "v2":{"score":10.0,"severity":"HIGH","vector":"AV:N"}}}
                """);
        var cvss = CvssExtractor.extract(payload);
        assertEquals("CRITICAL", cvss.severity());
        assertEquals("9.8", cvss.score().toPlainString());
        assertTrue(cvss.allVersions().has("v3.1"));
        assertTrue(cvss.allVersions().has("v2"));
    }

    @Test
    void extractsCpesWithoutDedupingAcrossSameUri() throws Exception {
        var payload = mapper.readTree("""
                {"cpes":[
                  {"cpe":"cpe:2.3:a:northwind:web_server:1.0:*:*:*:*:*:*:*","vendor":"northwind","product":"web_server"},
                  {"cpe":"cpe:2.3:a:northwind:web_server:1.0:*:*:*:*:*:*:*"}
                ]}
                """);
        var cpes = CpeExtractor.extract(payload);
        assertEquals(1, cpes.size());
        assertEquals("northwind", cpes.getFirst().vendor());
    }

    @Test
    void parseRejectsMissingDescription() throws Exception {
        var payload = mapper.readTree("{\"cveId\":\"CVE-2024-90099\"}");
        assertThrows(IngestionException.class, () -> CveDocumentParser.parse(payload, "manual"));
    }
}
