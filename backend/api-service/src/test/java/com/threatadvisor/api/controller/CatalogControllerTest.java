package com.threatadvisor.api.controller;

import com.threatadvisor.api.dto.PageResponse;
import com.threatadvisor.api.dto.catalog.FindingDetailResponse;
import com.threatadvisor.api.exception.ApiException;
import com.threatadvisor.api.exception.GlobalExceptionHandler;
import com.threatadvisor.api.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {FindingController.class, VulnerabilityController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalog;

    @Test
    void findingNotFoundIsFriendly() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalog.finding(id)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "FINDING_NOT_FOUND", "Unable to load that finding."));
        mockMvc.perform(get("/api/findings/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FINDING_NOT_FOUND"));
    }

    @Test
    void findingDetailReturnsMatchText() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalog.finding(id)).thenReturn(new FindingDetailResponse(
                id, "OPEN", "CVE-2021-44228", UUID.randomUUID(), UUID.randomUUID(),
                "web-prod-01", "PRODUCTION", "Linux", "EXACT_VERSION_MATCH", "HIGH",
                "Asset web-prod-01 is affected because it runs Apache HTTP Server 2.4.49.",
                null, null));
        mockMvc.perform(get("/api/findings/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchExplanation").value(org.hamcrest.Matchers.containsString("Apache HTTP Server")))
                .andExpect(jsonPath("$.cveId").value("CVE-2021-44228"));
    }

    @Test
    void vulnerabilityListIsPaged() throws Exception {
        when(catalog.vulnerabilities(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        mockMvc.perform(get("/api/vulnerabilities?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }
}
