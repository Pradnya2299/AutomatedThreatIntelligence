package com.threatadvisor.api.controller;

import com.threatadvisor.api.dto.PageResponse;
import com.threatadvisor.api.dto.catalog.AssetDetailResponse;
import com.threatadvisor.api.dto.catalog.AssetListItem;
import com.threatadvisor.api.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final CatalogService catalog;

    public AssetController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public PageResponse<AssetListItem> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.assets(page, size);
    }

    @GetMapping("/{id}")
    public AssetDetailResponse get(@PathVariable UUID id) {
        return catalog.asset(id);
    }
}
