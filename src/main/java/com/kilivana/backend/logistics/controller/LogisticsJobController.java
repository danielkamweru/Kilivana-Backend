package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logistics/jobs")
@RequiredArgsConstructor
public class LogisticsJobController {

    private final LogisticsService logisticsService;

    @PostMapping
    public ResponseEntity<ApiResponse<LogisticsJob>> createJob(@RequestBody LogisticsJob job) {
        LogisticsJob created = logisticsService.createJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LogisticsJob>> getJobById(@PathVariable Long id) {
        LogisticsJob job = logisticsService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getAllJobs(Pageable pageable) {
        List<LogisticsJob> jobs = logisticsService.getAllJobs();
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getJobsByDriver(@PathVariable Long driverId) {
        List<LogisticsJob> jobs = logisticsService.getJobsByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getJobsByOrder(@PathVariable Long orderId) {
        List<LogisticsJob> jobs = logisticsService.getJobsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getJobsByStatus(@PathVariable DeliveryStatus status) {
        List<LogisticsJob> jobs = logisticsService.getJobsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<LogisticsJob>> assignDriver(@PathVariable Long id, @RequestParam Long driverId) {
        LogisticsJob job = logisticsService.assignDriver(id, driverId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<LogisticsJob>> acceptJob(@PathVariable Long id, @RequestParam Long driverId) {
        LogisticsJob job = logisticsService.acceptJob(id, driverId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LogisticsJob>> updateJobStatus(@PathVariable Long id, @RequestParam DeliveryStatus status) {
        LogisticsJob job = logisticsService.updateJobStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<LogisticsJob>> cancelJob(@PathVariable Long id, @RequestBody String cancellationReason) {
        LogisticsJob job = logisticsService.cancelJob(id, cancellationReason);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        logisticsService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Job deleted successfully"));
    }
}
