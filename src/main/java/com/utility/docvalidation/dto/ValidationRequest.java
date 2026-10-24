package com.utility.docvalidation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/validate}.
 *
 * <p>Bean Validation enforces presence and a sane upper bound on length; the
 * document-type token is validated for support in the service layer so that an
 * unsupported type yields a message listing the supported types.
 *
 * @param documentType   one of PAN, CARD, AADHAAR (VID accepted as an alias), case-insensitive
 * @param documentNumber the value to validate
 */
@Schema(name = "ValidationRequest", description = "Document validation request")
public record ValidationRequest(

        @Schema(description = "Document type. Case-insensitive. VID is an alias for AADHAAR.",
                example = "PAN",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "documentType must not be blank")
        String documentType,

        @Schema(description = "The value to validate (echoed back as received).",
                example = "ABCPE1234F",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "documentNumber must not be blank")
        @Size(max = 64, message = "documentNumber must not exceed 64 characters")
        String documentNumber) {
}
