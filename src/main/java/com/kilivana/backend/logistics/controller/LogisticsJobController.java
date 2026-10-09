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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Delivery job management: creation, retrieval, driver assignment,
 * status transitions, delivery-code verification, cancellation and deletion.
 */
@Tag(name = "Logistics · Delivery Jobs", description = "Delivery job creation, driver assignment and status")
@RestController
@RequestMapping("/api/v1/logistics/jobs")
@RequiredArgsConstructor
public class LogisticsJobController {

    private final LogisticsService logisticsService;

    @Operation(summary = "Create a delivery job",
            description = "Creates a delivery job for an existing order and sends a freshly generated delivery code to the buyer. When no payout amount is supplied it is derived from the order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Delivery job created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input, e.g. a payout that does not match the order subtotal less the platform fee"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<LogisticsJob>> createJob(@RequestBody LogisticsJob job) {
        LogisticsJob created = logisticsService.createJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "Get a delivery job by ID", description = "Returns a single delivery job.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery job found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LogisticsJob>> getJobById(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id) {
        LogisticsJob job = logisticsService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "List all delivery jobs", description = "Returns every delivery job.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery jobs returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getAllJobs(Pageable pageable) {
        List<LogisticsJob> jobs = logisticsService.getAllJobs();
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @Operation(summary = "List jobs of a driver", description = "Returns the delivery jobs assigned to one driver, newest first.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery jobs returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getJobsByDriver(
            @Parameter(description = "ID of the driver") @PathVariable Long driverId) {
        List<LogisticsJob> jobs = logisticsService.getJobsByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @Operation(summary = "List jobs of an order", description = "Returns the delivery jobs belonging to one order.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery jobs returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getJobsByOrder(
            @Parameter(description = "ID of the order") @PathVariable Long orderId) {
        List<LogisticsJob> jobs = logisticsService.getJobsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @Operation(summary = "List jobs by status", description = "Returns the delivery jobs in a given status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery jobs returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown delivery status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getJobsByStatus(
            @Parameter(description = "Delivery status to filter by, e.g. picked_up or in_transit", example = "in_transit") @PathVariable String status) {
        List<LogisticsJob> jobs = logisticsService.getJobsByStatus(DeliveryStatus.from(status));
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @Operation(summary = "Assign a driver to a job (PUT)",
            description = "Assigns a driver to a job that is still pending assignment and moves the job to assigned.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Driver assigned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Job is not pending assignment, or the user is not a driver"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job or driver not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<LogisticsJob>> assignDriver(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "ID of the driver to assign") @RequestParam Long driverId) {
        LogisticsJob job = logisticsService.assignDriver(id, driverId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "Assign a driver to a job (POST)",
            description = "POST alias of PUT /{id}/assign; assigns a driver to a job that is still pending assignment.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Driver assigned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Job is not pending assignment, or the user is not a driver"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job or driver not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<LogisticsJob>> assignDriverPost(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "ID of the driver to assign") @RequestParam Long driverId) {
        return assignDriver(id, driverId);
    }

    @Operation(summary = "Accept an assigned job (PUT)",
            description = "Marks an assigned job as accepted by the driver it was assigned to.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Driver is not assigned to this job, or the job is not in assigned status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<LogisticsJob>> acceptJob(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "ID of the driver accepting the job") @RequestParam Long driverId) {
        LogisticsJob job = logisticsService.acceptJob(id, driverId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "Accept an assigned job (POST)",
            description = "POST alias of PUT /{id}/accept; marks an assigned job as accepted by the driver it was assigned to.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Driver is not assigned to this job, or the job is not in assigned status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<LogisticsJob>> acceptJobPost(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "ID of the driver accepting the job") @RequestParam Long driverId) {
        return acceptJob(id, driverId);
    }

    @Operation(summary = "Update job status (PUT)",
            description = "Moves a job to the next status in its delivery pipeline; illegal transitions are rejected.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown status, or the job cannot move to it from its current status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LogisticsJob>> updateJobStatus(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "Target delivery status, e.g. picked_up or delivered", example = "delivered") @RequestParam String status) {
        LogisticsJob job = logisticsService.updateJobStatus(id, DeliveryStatus.from(status));
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "Update job status (POST)",
            description = "POST alias of PUT /{id}/status; moves a job to the next status in its delivery pipeline.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown status, or the job cannot move to it from its current status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LogisticsJob>> updateJobStatusPost(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "Target delivery status, e.g. picked_up or delivered", example = "delivered") @RequestParam String status) {
        return updateJobStatus(id, status);
    }

    @Operation(summary = "Verify the delivery handover code",
            description = "Confirms the customer-supplied six-digit delivery code. Wrong codes are counted and the code locks temporarily after too many failures.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery code verified"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Job has no delivery code, the code has expired, or the code is incorrect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many incorrect codes; the delivery code is temporarily locked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/otp/verify")
    public ResponseEntity<ApiResponse<LogisticsJob>> verifyDeliveryOtp(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @Parameter(description = "Six-digit delivery handover code sent to the buyer", example = "123456") @RequestParam String otp) {
        LogisticsJob job = logisticsService.verifyDeliveryOtp(id, otp);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "Cancel a job", description = "Cancels a delivery job and records the cancellation reason.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<LogisticsJob>> cancelJob(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id,
            @RequestBody String cancellationReason) {
        LogisticsJob job = logisticsService.cancelJob(id, cancellationReason);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "Delete a job", description = "Deletes a delivery job.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @Parameter(description = "ID of the delivery job") @PathVariable Long id) {
        logisticsService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Job deleted successfully"));
    }
}
