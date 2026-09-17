package com.financetracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest")
public record LoginRequest(


        @NotBlank @Email @Schema(example = "bharath@example.com") String email,

        @NotBlank @Schema(example = "s3cure-passw0rd") String password
) {
}
