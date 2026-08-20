package com.threatadvisor.api.controller;

import com.threatadvisor.api.dto.dashboard.DashboardSummaryResponse;
import com.threatadvisor.api.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void summaryReturnsCounts() throws Exception {
        when(dashboardService.summary()).thenReturn(new DashboardSummaryResponse(
                4, 12, 27, 41, 18, 7, 9, 9,
                new DashboardSummaryResponse.RiskDistribution(7, 5, 3, 1),
                List.of(),
                List.of()));
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalVulnerabilities").value(4))
                .andExpect(jsonPath("$.affectedAssets").value(18))
                .andExpect(jsonPath("$.aiRemediationPlans").value(9));
    }
}
