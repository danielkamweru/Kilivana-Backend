package com.kilivana.backend.common.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmailOtpRequest {
    @NotBlank @Email @Size(max=254) private String email;
}
