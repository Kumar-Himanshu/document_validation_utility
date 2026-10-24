package com.utility.docvalidation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Structured error body returned for 4xx and 5xx responses.
 *
 * @param timestamp when the error occurred (ISO-8601 with offset)
 * @param status    the HTTP status code
 * @param error     the HTTP status reason phrase
 * @param message   a human-readable, non-sensitive explanation
 * @param path      the request path that produced the error
 */
@Schema(name = "ErrorResponse", description = "Structured error body")
public record ErrorResponse(

        @Schema(example = "2026-10-24T10:15:30.123+05:30")
        OffsetDateTime timestamp,

        @Schema(example = "400")
        int status,

        @Schema(example = "Bad Request")
        String error,

        @Schema(example = "Unsupported documentType. Supported types: PAN, CARD, AADHAAR, VID")
        String message,

        @Schema(example = "/api/v1/validate")
        String path) {
}
