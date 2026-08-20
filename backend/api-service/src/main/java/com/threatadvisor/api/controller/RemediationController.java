package com.threatadvisor.api.controller;

import com.threatadvisor.api.dto.PageResponse;
import com.threatadvisor.api.dto.catalog.RemediationListItem;
import com.threatadvisor.api.dto.catalog.RemediationPlanDto;
import com.threatadvisor.api.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/remediation")
public class RemediationController {

    private final CatalogService catalog;

    public RemediationController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public PageResponse<RemediationListItem> list(
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String risk,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.remediations(priority, status, risk, page, size);
    }

    @GetMapping("/{id}")
    public RemediationPlanDto get(@PathVariable UUID id) {
        return catalog.remediation(id);
    }
}
