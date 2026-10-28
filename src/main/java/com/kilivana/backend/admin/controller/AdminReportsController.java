package com.kilivana.backend.admin.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Administration · Reports", description = "Admin reporting and analytics")
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportsController {

    private final com.kilivana.backend.admin.service.AdminReportsService reports;

    @Operation(summary = "Report overview", description = "KPIs, sales trend and order counts for a date range.")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<com.kilivana.backend.admin.dto.ReportOverviewResponse>> overview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(reports.overview(from, to)));
    }

    @Operation(summary = "Disputes report", description = "Dispute totals, resolution time and outcomes.")
    @GetMapping("/disputes")
    public ResponseEntity<ApiResponse<com.kilivana.backend.admin.dto.DisputeReportResponse>> disputes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(reports.disputes(from, to)));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new com.kilivana.backend.common.exception.BadRequestException("from must be <= to");
        if (from.plusDays(366).isBefore(to)) throw new com.kilivana.backend.common.exception.BadRequestException("Range must be <= 366 days");
    }
}
