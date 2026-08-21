package com.threatadvisor.api.controller;

import com.threatadvisor.api.client.AiInvestigationClient;
import com.threatadvisor.api.config.CorrelationIdFilter;
import com.threatadvisor.api.dto.InvestigationCreateRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/investigations")
public class InvestigationController {

    private final AiInvestigationClient client;

    public InvestigationController(AiInvestigationClient client) {
        this.client = client;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(@Valid @RequestBody InvestigationCreateRequest request) {
        return client.create(request, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(@PathVariable UUID id) {
        return client.get(id, MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
