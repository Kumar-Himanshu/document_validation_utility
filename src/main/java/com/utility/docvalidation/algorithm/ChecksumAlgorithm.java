package com.utility.docvalidation.algorithm;

/**
 * A reusable checksum algorithm that verifies the integrity of a digit string.
 *
 * <p>This interface is intentionally narrow (Interface Segregation): it is
 * concerned only with verifying a checksum and knows nothing about document
 * types, HTTP or formatting. Implementations must be stateless and thread-safe.
 */
public interface ChecksumAlgorithm {

    /**
     * Verifies the checksum of the supplied value.
     *
     * @param digits a string expected to contain digits only; implementations
     *               must tolerate (reject gracefully) any non-conforming input
     * @return {@code true} if the value satisfies the checksum, otherwise {@code false}
     */
    boolean isValid(String digits);
}
