package com.kilivana.backend.common.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

/** Intentionally no generated toString: submitted codes must not be logged. */
@Getter @Setter
public class EmailOtpVerifyRequest {
    @NotNull private UUID challengeId;
    @NotBlank @Email @Size(max=254) private String email;
    @NotNull @Pattern(regexp="[0-9]{6}",message="Code must contain exactly six digits") private String code;
}
