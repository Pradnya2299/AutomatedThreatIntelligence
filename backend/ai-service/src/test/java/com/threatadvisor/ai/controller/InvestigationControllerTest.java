package com.threatadvisor.ai.controller;

import com.threatadvisor.ai.agent.common.InvestigationStatus;
import com.threatadvisor.ai.agent.common.SecurityInvestigationContext;
import com.threatadvisor.ai.agent.orchestrator.SecurityOrchestrator;
import com.threatadvisor.ai.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvestigationController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityOrchestrator orchestrator;

    @Test
    void createReturnsInvestigation() throws Exception {
        UUID id = UUID.randomUUID();
        when(orchestrator.startInvestigation(eq("CVE-2021-44228"), any())).thenReturn(
                SecurityInvestigationContext.builder()
                        .investigationId(id)
                        .cveId("CVE-2021-44228")
                        .status(InvestigationStatus.RUNNING)
                        .build());
        mockMvc.perform(post("/api/v1/investigations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cveId\":\"CVE-2021-44228\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investigationId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void getUnknownIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orchestrator.get(id)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/investigations/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVESTIGATION_NOT_FOUND"));
    }

    @Test
    void getRejectsInvalidId() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/{id}", "{investigationId}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
