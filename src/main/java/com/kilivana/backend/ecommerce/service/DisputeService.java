package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.ecommerce.dto.DisputeRequest;
import com.kilivana.backend.ecommerce.dto.DisputeResponse;
import com.kilivana.backend.ecommerce.entity.Dispute;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;

    @Transactional
    public DisputeResponse createDispute(DisputeRequest request) {
        Dispute dispute = Dispute.builder()
                .orderId(request.getOrderId())
                .raisedBy(request.getRaisedBy())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : DisputeStatus.OPEN)
                .build();
        Dispute saved = disputeRepository.save(dispute);
        return mapToResponse(saved);
    }

    public DisputeResponse getDisputeById(Long id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        return mapToResponse(dispute);
    }

    public List<DisputeResponse> getDisputesByOrder(Long orderId) {
        return disputeRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DisputeResponse> getDisputesByUser(Long userId) {
        return disputeRepository.findByRaisedBy(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DisputeResponse updateDisputeStatus(Long id, DisputeStatus status) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        dispute.setStatus(status);
        return mapToResponse(disputeRepository.save(dispute));
    }

    @Transactional
    public DisputeResponse resolveDispute(Long id, String resolution) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
        dispute.setResolution(resolution);
        dispute.setStatus(DisputeStatus.RESOLVED);
        return mapToResponse(disputeRepository.save(dispute));
    }

    private DisputeResponse mapToResponse(Dispute dispute) {
        return DisputeResponse.builder()
                .id(dispute.getId())
                .orderId(dispute.getOrderId())
                .raisedBy(dispute.getRaisedBy())
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .resolution(dispute.getResolution())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }
}
