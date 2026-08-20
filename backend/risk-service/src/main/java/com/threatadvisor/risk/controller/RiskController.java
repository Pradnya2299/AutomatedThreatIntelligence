package com.threatadvisor.risk.controller;

import com.threatadvisor.risk.config.CorrelationIdFilter;
import com.threatadvisor.risk.dto.RiskRunResponse;
import com.threatadvisor.risk.service.RiskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/risk")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/run/{findingId}")
    public ResponseEntity<RiskRunResponse> run(
            @PathVariable UUID findingId,
            @RequestHeader(value = CorrelationIdFilter.HEADER, required = false) UUID correlationId) {
        RiskService.RiskOutcome outcome = riskService.runForFinding(findingId, correlationId);
        return ResponseEntity.ok(new RiskRunResponse(
                outcome.status(),
                outcome.findingId(),
                outcome.riskAssessmentId(),
                outcome.eventId(),
                outcome.result() == null ? null : outcome.result().score(),
                outcome.result() == null ? null : outcome.result().level()));
    }
}
