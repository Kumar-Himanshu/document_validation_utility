package com.utility.docvalidation.controller;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.dto.ValidationRequest;
import com.utility.docvalidation.dto.ValidationResponse;
import com.utility.docvalidation.service.DocumentValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single public API for document validation.
 *
 * <p>This controller deals only with HTTP: binding and validating the request,
 * delegating to the {@link DocumentValidationService} and returning the result.
 * It contains no validation logic and knows nothing about specific document
 * types, so new types never require a controller change (Open/Closed).
 */
@RestController
@RequestMapping("/api/v1/validate")
@Tag(name = "Validation", description = "Format and checksum validation for supported document types")
public class ValidationController {

    private final DocumentValidationService validationService;

    /**
     * @param validationService the service abstraction (constructor injected)
     */
    public ValidationController(DocumentValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Validates a single document value.
     *
     * <p>A value that merely fails validation is <em>not</em> an error: it
     * returns 200 with {@code isValid=false}. Malformed requests (blank fields,
     * unsupported type) return 400.
     *
     * @param request the validation request
     * @return the uniform validation result
     */
    @Operation(summary = "Validate a document value",
            description = "Validates format and checksum for the given type. "
                    + "A value that fails validation returns 200 with isValid=false.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation performed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ValidationResponse.class),
                            examples = {
                                    @ExampleObject(name = "valid",
                                            value = "{\"documentNumber\":\"ABCPE1234F\",\"isValid\":true}"),
                                    @ExampleObject(name = "invalid",
                                            value = "{\"documentNumber\":\"ABCZE1234F\",\"isValid\":false}")
                            })),
            @ApiResponse(responseCode = "400", description = "Malformed request", content = @Content),
            @ApiResponse(responseCode = "500", description = "Unexpected failure", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidationResponse validate(@Valid @RequestBody ValidationRequest request) {
        return validationService.validate(request);
    }

    /**
     * Lists the document types currently supported by the service.
     *
     * @return the supported type names
     */
    @Operation(summary = "List supported document types")
    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> supportedTypes() {
        return validationService.supportedTypes().stream()
                .map(DocumentType::name)
                .sorted()
                .toList();
    }
}
