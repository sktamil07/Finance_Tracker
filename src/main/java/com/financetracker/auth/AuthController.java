package com.financetracker.auth;

import com.financetracker.auth.dto.LoginRequest;
import com.financetracker.auth.dto.RefreshRequest;
import com.financetracker.auth.dto.RegisterRequest;
import com.financetracker.auth.dto.TokenResponse;
import com.financetracker.auth.service.AuthService;
import com.financetracker.common.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registration, login, and token refresh. The only public endpoints.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user and seed their default categories")
    @ApiResponse(responseCode = "201", description = "Account created; token pair returned")
    @ApiResponse(responseCode = "409", description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Exchange email + password for an access/refresh token pair")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Exchange a refresh token for a new token pair",
            description = "The pair is rotated: both tokens are new. An access token is rejected here.")
    @ApiResponse(responseCode = "200", description = "New token pair issued")
    @ApiResponse(responseCode = "401", description = "Invalid, expired, or wrong-type token",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }
}
