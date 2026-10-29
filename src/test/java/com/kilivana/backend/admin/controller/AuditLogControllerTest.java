package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AuditLogResponse;
import com.kilivana.backend.admin.service.AuditLogService;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.security.AccountAccessValidator;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static com.kilivana.backend.support.JwtTestSupport.asAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard for the audit-log search endpoint.
 *
 * <p>Calling {@code GET /api/v1/admin/audit-logs} with no pagination params used to hand
 * Spring's {@code Unpaging} sentinel straight into the response, and Jackson threw on
 * {@code Unpaging.getPageNumber()} — a plain "list me everything" call came back 500.
 */
@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(AuditLogController.class)
@org.springframework.context.annotation.Import(SecurityConfig.class)
class AuditLogControllerTest {

    @MockBean
    private AccountAccessValidator accountAccessValidator;

    @MockBean
    private AuditLogService auditLogService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private AuditLogResponse sample() {
        return AuditLogResponse.builder()
                .id(1L)
                .actorId(7L)
                .action("SUSPEND")
                .entityType("USER")
                .entityId(50L)
                .metadata("{\"reason\":\"spam\"}")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30))
                .build();
    }

    @Test
    void search_withoutPaginationParams_returnsPagedJsonNot500() throws Exception {
        when(auditLogService.searchAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sample()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].action").value("SUSPEND"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void search_withExplicitPageAndSize_isForwarded() throws Exception {
        when(auditLogService.searchAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("page", "1")
                        .param("size", "5")
                        .with(asAdmin(jwtService, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}