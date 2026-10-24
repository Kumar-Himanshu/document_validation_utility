package com.utility.docvalidation.validator;

import com.utility.docvalidation.dto.DocumentType;

/**
 * Strategy interface implemented by every document-type validator.
 *
 * <p>All implementations are substitutable (Liskov): given a raw value they
 * return a boolean verdict and never throw for ordinary invalid input. Each
 * implementation declares exactly one {@link DocumentType} it is responsible
 * for, which the registry uses to route requests.
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface DocumentValidator {

    /**
     * The single document type this validator handles.
     *
     * @return the supported type
     */
    DocumentType type();

    /**
     * Validates the given raw value according to this type's rules, performing
     * any required normalisation (trimming, case folding, stripping separators)
     * internally.
     *
     * @param value the raw input value (may be null or blank)
     * @return {@code true} if the value is valid for this type, otherwise {@code false}
     */
    boolean validate(String value);
}
