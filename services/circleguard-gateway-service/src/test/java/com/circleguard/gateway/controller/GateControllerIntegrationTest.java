package com.circleguard.gateway.controller;

import com.circleguard.gateway.service.QrValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GateController.class)
class GateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrValidationService validationService;

    @Test
    void validate_validToken_returnsGreenAccess() throws Exception {
        when(validationService.validateToken("valid-token"))
                .thenReturn(new QrValidationService.ValidationResult(true, "GREEN", "Welcome to Campus"));

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("GREEN"))
                .andExpect(jsonPath("$.message").value("Welcome to Campus"));
    }

    @Test
    void validate_invalidToken_returnsRedDenied() throws Exception {
        when(validationService.validateToken("invalid-token"))
                .thenReturn(new QrValidationService.ValidationResult(false, "RED", "Invalid or Expired Token"));

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"invalid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("RED"));
    }
}
