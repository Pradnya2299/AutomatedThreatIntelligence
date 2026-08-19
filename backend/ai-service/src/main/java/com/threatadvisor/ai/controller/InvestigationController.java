package com.threatadvisor.ai.controller;

import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.orchestrator.SecurityOrchestrator;
import com.threatadvisor.ai.config.CorrelationIdFilter;
import com.threatadvisor.ai.dto.InvestigationRequest;
import com.threatadvisor.ai.dto.InvestigationResponse;
import com.threatadvisor.ai.exception.AiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {

    private final SecurityOrchestrator orchestrator;

    public InvestigationController(SecurityOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public InvestigationResponse create(
            @Valid @RequestBody InvestigationRequest request,
            @RequestHeader(value = CorrelationIdFilter.HEADER, required = false) UUID correlationId) {
        SecurityInvestigationContext context = orchestrator.startInvestigation(request.cveId(), correlationId);
        return InvestigationResponse.from(context);
    }

    @GetMapping("/{id}")
    public InvestigationResponse get(@PathVariable UUID id) {
        return orchestrator.get(id)
                .map(InvestigationResponse::from)
                .orElseThrow(() -> new AiException(HttpStatus.NOT_FOUND, "INVESTIGATION_NOT_FOUND", "Unknown investigation"));
    }
}
