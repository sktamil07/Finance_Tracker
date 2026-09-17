package com.financetracker.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * The single error body shape returned by every failing endpoint.
 */
@Schema(name = "ApiError", description = "Consistent error body for every failed request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @Schema(example = "2026-04-12T09:31:04.221Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Validation failed for 2 field(s)") String message,
        @Schema(example = "/api/v1/expenses") String path,
        @Schema(description = "Present only for validation failures") List<FieldError> fieldErrors
) {

    @Schema(name = "FieldError")
    public record FieldError(
            @Schema(example = "amount") String field,
            @Schema(example = "must be greater than 0") String message
    ) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    }
}
