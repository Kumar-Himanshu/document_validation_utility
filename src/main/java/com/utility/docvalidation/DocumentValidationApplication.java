package com.utility.docvalidation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Document Validation Utility service.
 *
 * <p>The service exposes a single public API that validates the format and
 * checksum integrity of identity and financial identifiers (PAN, Card and
 * Aadhaar/VID). It is stateless and designed so that new document types can be
 * added by registering a new validator, without changing the API contract.
 */
@SpringBootApplication
public class DocumentValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentValidationApplication.class, args);
    }
}
