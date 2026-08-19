package com.threatadvisor.ai.coderemediation;

import com.threatadvisor.ai.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CodeRemediationController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(GlobalExceptionHandler.class)
class CodeRemediationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodeRemediationService service;

    @Test
    void unknownRemediationIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/remediations/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REMEDIATION_NOT_FOUND"));
    }
}
