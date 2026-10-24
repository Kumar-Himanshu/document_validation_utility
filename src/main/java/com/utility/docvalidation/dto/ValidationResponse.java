package com.utility.docvalidation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for {@code POST /api/v1/validate}.
 *
 * <p>The structure is identical for every document type, as required by the
 * BRD. The document number is echoed back exactly as received.
 *
 * @param documentNumber the input value, echoed back as received
 * @param isValid        {@code true} if the value passes the rules for its type
 */
@Schema(name = "ValidationResponse", description = "Uniform validation result")
public record ValidationResponse(

        @Schema(description = "The input value, echoed back as received.", example = "ABCPE1234F")
        String documentNumber,

        @Schema(description = "True if the value passes validation for the given type.", example = "true")
        boolean isValid) {
}
