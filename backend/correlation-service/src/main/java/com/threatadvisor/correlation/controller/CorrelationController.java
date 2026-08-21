package com.threatadvisor.correlation.controller;

import com.threatadvisor.correlation.config.CorrelationIdFilter;
import com.threatadvisor.correlation.dto.CorrelationRunResponse;
import com.threatadvisor.correlation.service.CorrelationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/correlation")
public class CorrelationController {

    private final CorrelationService correlationService;

    public CorrelationController(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    @PostMapping("/run/{cveId}")
    public ResponseEntity<CorrelationRunResponse> run(
            @PathVariable String cveId,
            @RequestHeader(value = CorrelationIdFilter.HEADER, required = false) UUID correlationId) {
        CorrelationService.CorrelationOutcome outcome = correlationService.runForCve(cveId, correlationId);
        return ResponseEntity.ok(new CorrelationRunResponse(
                outcome.status(),
                outcome.cveId(),
                outcome.vulnerabilityId(),
                outcome.eventId(),
                outcome.findingsCreated(),
                outcome.findingsUpdated(),
                outcome.matchesEvaluated()));
    }
}
