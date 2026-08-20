package com.threatadvisor.api.controller;

import com.threatadvisor.api.client.AiCodeRemediationClient;
import com.threatadvisor.api.config.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CodeRemediationController {

    private final AiCodeRemediationClient client;

    public CodeRemediationController(AiCodeRemediationClient client) {
        this.client = client;
    }

    @PostMapping(value = "/investigations/{investigationId}/remediation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> start(@PathVariable UUID investigationId, @RequestBody(required = false) String body) {
        return client.start(investigationId, body, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @GetMapping(value = "/investigations/{investigationId}/remediation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> latest(@PathVariable UUID investigationId) {
        return client.latest(investigationId, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @GetMapping(value = "/remediations/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(@PathVariable UUID id) {
        return client.get(id, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @GetMapping(value = "/remediations/{id}/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> diff(@PathVariable UUID id) {
        return client.diff(id, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @GetMapping(value = "/remediations/{id}/validations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validations(@PathVariable UUID id) {
        return client.validations(id, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @GetMapping(value = "/remediations/{id}/pull-request", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> pullRequest(@PathVariable UUID id) {
        return client.pullRequest(id, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @PostMapping(value = "/remediations/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> approve(@PathVariable UUID id, @RequestBody(required = false) String body) {
        return client.approve(id, body, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @PostMapping(value = "/remediations/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> reject(@PathVariable UUID id, @RequestBody(required = false) String body) {
        return client.reject(id, body, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @PostMapping(value = "/remediations/{id}/request-changes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> requestChanges(@PathVariable UUID id, @RequestBody(required = false) String body) {
        return client.requestChanges(id, body, MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
