package com.circleguard.auth.controller;

import com.circleguard.auth.service.QrTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrTokenController.class)
class QrTokenControllerIntegrationTest {

    private static final UUID ANONYMOUS_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrTokenService qrService;

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void generateQrToken_authenticatedUser_returnsTokenAndExpiry() throws Exception {
        when(qrService.generateQrToken(ANONYMOUS_ID)).thenReturn("signed-qr-token");

        mockMvc.perform(get("/api/v1/auth/qr/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").value("signed-qr-token"))
                .andExpect(jsonPath("$.expiresIn").value("60"));
    }

    @Test
    void generateQrToken_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/qr/generate"))
                .andExpect(status().isUnauthorized());
    }
}
