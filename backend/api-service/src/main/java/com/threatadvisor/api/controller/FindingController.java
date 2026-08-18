package com.threatadvisor.api.controller;

import com.threatadvisor.api.dto.PageResponse;
import com.threatadvisor.api.dto.catalog.FindingDetailResponse;
import com.threatadvisor.api.dto.catalog.FindingListItem;
import com.threatadvisor.api.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/findings")
public class FindingController {

    private final CatalogService catalog;

    public FindingController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public PageResponse<FindingListItem> list(
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String asset,
            @RequestParam(required = false) String cve,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.findings(risk, status, asset, cve, page, size);
    }

    @GetMapping("/{id}")
    public FindingDetailResponse get(@PathVariable UUID id) {
        return catalog.finding(id);
    }
}
