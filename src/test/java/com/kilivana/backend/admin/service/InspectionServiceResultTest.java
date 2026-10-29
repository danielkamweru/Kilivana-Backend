package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.InspectionRequest;
import com.kilivana.backend.admin.entity.Inspection;
import com.kilivana.backend.admin.repository.InspectionEvidenceImageRepository;
import com.kilivana.backend.admin.repository.InspectionRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.farm.repository.FarmRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceResultTest {

    @Mock private InspectionRepository inspectionRepository;
    @Mock private InspectionEvidenceImageRepository evidenceImageRepository;
    @Mock private ImageStorage imageStorage;
    @Mock private FarmRepository farmRepository;
    @Mock private UserRepository userRepository;

    private InspectionService inspectionService;

    @BeforeEach
    void setUp() {
        inspectionService = new InspectionService(inspectionRepository, evidenceImageRepository,
                imageStorage, farmRepository, userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED", "CHANGES_REQUIRED"})
    void submittingFindingsCompletesInspection(String result) {
        Inspection inspection = Inspection.builder()
                .id(20L)
                .inspectorId(3L)
                .targetType("PRODUCT")
                .targetId(5L)
                .status(InspectionStatus.IN_PROGRESS)
                .build();
        when(inspectionRepository.findById(20L)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = inspectionService.updateInspectionResult(20L, result,
                InspectionRequest.builder().build());

        assertEquals(InspectionStatus.COMPLETED, response.getStatus());
        assertEquals(InspectionResult.valueOf(result), response.getResult());
        assertNotNull(response.getInspectedAt());
    }
}
