package com.utility.docvalidation.exception;

/**
 * Thrown when a request specifies a document type that is not supported.
 *
 * <p>Handled as an HTTP 400 by the global exception handler, with a message
 * that lists the supported types.
 */
public class UnsupportedDocumentTypeException extends RuntimeException {

    /**
     * @param message a human-readable, non-sensitive message listing supported types
     */
    public UnsupportedDocumentTypeException(String message) {
        super(message);
    }
}
