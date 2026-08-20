package com.threatadvisor.api.controller;

import com.threatadvisor.api.client.AiCodeRemediationClient;
import com.threatadvisor.api.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CodeRemediationController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class CodeRemediationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiCodeRemediationClient client;

    @Test
    void startProxiesAiService() throws Exception {
        UUID investigationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(client.start(any(), any(), any())).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"currentState\":\"AWAITING_APPROVAL\",\"status\":\"PATCH_READY_FOR_REVIEW\"}"));
        mockMvc.perform(post("/api/v1/investigations/" + investigationId + "/remediation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState").value("AWAITING_APPROVAL"));
    }

    @Test
    void getProxiesAiService() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(client.get(any(), any())).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"remediationId\":\"" + id + "\"}"));
        mockMvc.perform(get("/api/v1/remediations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remediationId").value(id.toString()));
    }
}
