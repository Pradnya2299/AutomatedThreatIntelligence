package com.threatadvisor.api.controller;

import com.threatadvisor.api.client.AiInvestigationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvestigationController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiInvestigationClient client;

    @Test
    void createProxiesAiService() throws Exception {
        when(client.create(any(), any())).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"investigationId\":\"11111111-1111-1111-1111-111111111111\",\"status\":\"COMPLETED\"}"));
        mockMvc.perform(post("/api/v1/investigations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cveId\":\"CVE-2021-44228\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getProxiesAiService() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(client.get(any(), any())).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"investigationId\":\"" + id + "\",\"status\":\"COMPLETED\"}"));
        mockMvc.perform(get("/api/v1/investigations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investigationId").value(id.toString()));
    }
}
