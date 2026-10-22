package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.dto.StkPushInitiateRequest;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.dto.StkPushResponse;
import com.kilivana.backend.ecommerce.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The checkout payment route: an authenticated buyer asks for the STK Push on their
 * own order. The route is buyer-scoped, so it must not answer an anonymous caller
 * and must not let a caller pay for an order that is not theirs.
 */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @MockBean private com.kilivana.backend.security.AccountAccessValidator accountAccessValidator;
    @MockBean private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    private static UsernamePasswordAuthenticationToken buyer(long id) {
        return new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
    }

    @Test
    void buyerStkPush_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/payments/stk-push/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":123,\"phoneNumber\":\"254712345678\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void buyerStkPush_returnsTheInitiatedPayment() throws Exception {
        StkPushResponse daraja = new StkPushResponse();
        daraja.setCheckoutRequestID("ws_CO_1");
        daraja.setMerchantRequestID("mr-1");
        daraja.setResponseCode("0");
        StkPushInitiateResponse response = new StkPushInitiateResponse();
        response.setPaymentId(99L);
        response.setOrderId(123L);
        response.setAmount(new BigDecimal("1500.00"));
        response.setStatus("pending");
        response.setDarajaResponse(daraja);
        response.setCreatedAt(LocalDateTime.now());
        when(paymentService.initiateStkPushForBuyer(eq(2L), eq(UserRole.BUYER), any(StkPushInitiateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/stk-push/buyer")
                        .with(authentication(buyer(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":123,\"phoneNumber\":\"254712345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(99))
                .andExpect(jsonPath("$.data.orderId").value(123))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.darajaResponse.checkoutRequestID").value("ws_CO_1"));

        verify(paymentService).initiateStkPushForBuyer(eq(2L), eq(UserRole.BUYER), any(StkPushInitiateRequest.class));
    }
}
