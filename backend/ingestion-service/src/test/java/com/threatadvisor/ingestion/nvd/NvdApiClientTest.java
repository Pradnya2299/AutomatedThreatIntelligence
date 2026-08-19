package com.threatadvisor.ingestion.nvd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatadvisor.ingestion.config.NvdProperties;
import com.threatadvisor.ingestion.normalization.CveDocumentParser;
import com.threatadvisor.ingestion.normalization.NormalizedVulnerability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class NvdApiClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MockRestServiceServer server;
    private NvdApiClient client;

    @BeforeEach
    void setup() {
        RestTemplate template = new RestTemplate();
        server = MockRestServiceServer.bindTo(template).build();
        NvdProperties properties = new NvdProperties();
        properties.setApiBaseUrl("https://services.nvd.nist.gov/rest/json/cves/2.0");
        properties.setMinIntervalWithoutKeyMs(0);
        properties.setMinIntervalWithKeyMs(0);
        client = new NvdApiClient(RestClient.builder(template), mapper, properties);
    }

    @Test
    void getCveSuccess() throws Exception {
        String body = new String(getClass().getResourceAsStream("/nvd/cve-2021-44228.json").readAllBytes());
        server.expect(requestTo("https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2021-44228"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        JsonNode node = client.getCve("CVE-2021-44228").orElseThrow();
        var documents = NvdDocumentAdapter.extractCveDocuments(node);
        NormalizedVulnerability normalized = CveDocumentParser.parse(documents.getFirst(), "nvd");
        assertEquals("CVE-2021-44228", normalized.cveId());
        assertEquals("CRITICAL", normalized.severity());
        assertEquals("10.0", normalized.cvssScore().toPlainString());
        assertTrue(normalized.cwes().contains("CWE-502"));
        assertTrue(normalized.fixedVersions().contains("2.17.0"));
        server.verify();
    }

    @Test
    void getCve404() {
        server.expect(requestTo("https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2099-0000"))
                .andRespond(withStatus(NOT_FOUND));
        NvdApiException ex = assertThrows(NvdApiException.class, () -> client.getCve("CVE-2099-0000"));
        assertEquals("NVD_NOT_FOUND", ex.getCode());
    }

    @Test
    void invalidJson() {
        server.expect(requestTo("https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2021-44228"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        NvdApiException ex = assertThrows(NvdApiException.class, () -> client.getCve("CVE-2021-44228"));
        assertEquals("NVD_INVALID_JSON", ex.getCode());
    }

    @Test
    void timeoutMappedFromServerError() {
        server.expect(requestTo("https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=CVE-2021-44228"))
                .andRespond(withServerError());
        NvdApiException ex = assertThrows(NvdApiException.class, () -> client.getCve("CVE-2021-44228"));
        assertEquals("NVD_UNAVAILABLE", ex.getCode());
    }

    @Test
    void paginationUsesStartIndex() {
        String page = """
                {"resultsPerPage":1,"startIndex":0,"totalResults":2,"vulnerabilities":[]}
                """;
        server.expect(requestTo(
                        "https://services.nvd.nist.gov/rest/json/cves/2.0?lastModStartDate=2021-12-01T00:00:00.000&lastModEndDate=2021-12-02T00:00:00.000&startIndex=0&resultsPerPage=100"))
                .andRespond(withSuccess(page, MediaType.APPLICATION_JSON));
        var result = client.getModifiedSince(
                Instant.parse("2021-12-01T00:00:00Z"), Instant.parse("2021-12-02T00:00:00Z"), 0);
        assertEquals(2, result.totalResults());
        assertTrue(result.hasMore());
        server.verify();
    }
}
