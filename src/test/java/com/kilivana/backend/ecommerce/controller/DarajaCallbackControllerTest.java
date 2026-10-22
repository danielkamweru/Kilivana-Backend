package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.dto.DarajaCallbackRequest;
import com.kilivana.backend.dto.DarajaCallbackResponse;
import com.kilivana.backend.service.DarajaCallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Safaricom posts the STK Push result to the callback without any credentials, so the
 * endpoint has to be reachable without a token — and it has to acknowledge quickly,
 * because a slow or rejected callback is retried by Safaricom.
 */
@WebMvcTest(DarajaCallbackController.class)
@Import(SecurityConfig.class)
class DarajaCallbackControllerTest {

    @MockBean private com.kilivana.backend.security.AccountAccessValidator accountAccessValidator;
    @MockBean private DarajaCallbackService callbackService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theCallbackIsReachableWithoutAToken() throws Exception {
        when(callbackService.processCallback(any(DarajaCallbackRequest.class), anyString()))
                .thenReturn(new DarajaCallbackResponse());

        mockMvc.perform(post("/api/v1/payments/daraja/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":{\"merchantRequestID\":\"mr-1\",\"checkoutRequestID\":\"ws_CO_1\",\"resultCode\":0}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resultCode").value(0));

        verify(callbackService).processCallback(any(DarajaCallbackRequest.class), anyString());
    }
}
