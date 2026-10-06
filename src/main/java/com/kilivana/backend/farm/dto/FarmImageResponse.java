package com.kilivana.backend.farm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmImageResponse {
    private Long id;
    private String url;
    private Boolean isPrimary;
}
