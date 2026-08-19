package com.threatadvisor.ingestion.controller;

import com.threatadvisor.ingestion.dto.IngestionResponse;
import com.threatadvisor.ingestion.dto.NvdLookupResponse;
import com.threatadvisor.ingestion.exception.IngestionException;
import com.threatadvisor.ingestion.service.CveIngestionService;
import com.threatadvisor.ingestion.service.NvdIngestionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/internal/ingestion")
public class CveIngestionController {

    private static final Pattern FIXTURE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final CveIngestionService ingestionService;
    private final NvdIngestionService nvdIngestionService;

    public CveIngestionController(CveIngestionService ingestionService, NvdIngestionService nvdIngestionService) {
        this.ingestionService = ingestionService;
        this.nvdIngestionService = nvdIngestionService;
    }

    @PostMapping(value = "/cve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestionResponse> ingest(
            @RequestBody byte[] body,
            @RequestParam(name = "source", defaultValue = "manual") String source,
            @RequestHeader(value = "X-Correlation-Id", required = false) UUID correlationId) {
        IngestionResponse response = ingestionService.ingest(body, source, correlationId);
        HttpStatus status = "ACCEPTED".equals(response.status()) ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/cve/fixture/{name}")
    public ResponseEntity<IngestionResponse> ingestFixture(
            @PathVariable String name,
            @RequestHeader(value = "X-Correlation-Id", required = false) UUID correlationId) {
        if (!FIXTURE_NAME.matcher(name).matches()) {
            throw new IngestionException(HttpStatus.BAD_REQUEST, "INVALID_FIXTURE", "Fixture name is not allowed");
        }
        ClassPathResource resource = new ClassPathResource("cve-fixtures/" + name + ".json");
        if (!resource.exists()) {
            throw new IngestionException(HttpStatus.NOT_FOUND, "FIXTURE_NOT_FOUND", "Unknown fixture");
        }
        byte[] body;
        try (InputStream in = resource.getInputStream()) {
            body = in.readAllBytes();
        } catch (IOException ex) {
            throw new IngestionException(HttpStatus.INTERNAL_SERVER_ERROR, "FIXTURE_UNREADABLE", "Could not read fixture");
        }
        IngestionResponse response = ingestionService.ingest(body, "fixture", correlationId);
        HttpStatus status = "ACCEPTED".equals(response.status()) ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/nvd/cves/{cveId}")
    public ResponseEntity<NvdLookupResponse> lookupFromNvd(@PathVariable String cveId) {
        NvdLookupResponse response = nvdIngestionService.lookupCve(cveId);
        return ResponseEntity.ok(response);
    }
}
