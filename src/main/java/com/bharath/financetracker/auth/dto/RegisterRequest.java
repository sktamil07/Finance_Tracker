package com.bharath.financetracker.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest")
public record RegisterRequest(

        @NotBlank
        @Size(max = 100)
        @Schema(example = "Bharath")
        String name,

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(example = "bharath@example.com")
        String email,

        // BCrypt silently truncates beyond 72 bytes, so reject longer inputs rather than
        // pretend to honour them.
        @NotBlank
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        @Schema(example = "s3cure-passw0rd")
        String password
) {
}
