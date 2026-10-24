package com.utility.docvalidation.service;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.dto.ValidationRequest;
import com.utility.docvalidation.dto.ValidationResponse;
import java.util.Set;

/**
 * Service abstraction the controller depends on (Dependency Inversion).
 *
 * <p>It orchestrates validation: parsing the document type, routing to the
 * correct validator and shaping the uniform response. HTTP concerns stay in the
 * controller; validation rules stay in the validators.
 */
public interface DocumentValidationService {

    /**
     * Validates a request and returns the uniform result.
     *
     * @param request the validated request DTO
     * @return a response echoing the document number and reporting validity
     * @throws com.utility.docvalidation.exception.UnsupportedDocumentTypeException
     *         if the document type is unsupported
     */
    ValidationResponse validate(ValidationRequest request);

    /**
     * @return the document types currently supported by the service
     */
    Set<DocumentType> supportedTypes();
}
