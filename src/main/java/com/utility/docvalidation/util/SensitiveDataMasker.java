package com.utility.docvalidation.util;

/**
 * Masks sensitive values (PAN, card and Aadhaar/VID numbers) for safe logging.
 *
 * <p>Only the last four characters of a value are ever revealed; everything
 * before them is replaced with a fixed mask character. Raw values must never be
 * logged or persisted anywhere in the application.
 */
public final class SensitiveDataMasker {

    private static final char MASK_CHAR = '*';
    private static final int VISIBLE_SUFFIX = 4;

    private SensitiveDataMasker() {
        // Utility class; not instantiable.
    }

    /**
     * Masks a value, leaving only its final four characters visible.
     *
     * <ul>
     *   <li>{@code null} becomes {@code "null"}.</li>
     *   <li>A value of four characters or fewer is fully masked, so that short
     *       values never leak in whole.</li>
     * </ul>
     *
     * @param value the value to mask
     * @return the masked representation, safe to log
     */
    public static String mask(String value) {
        if (value == null) {
            return "null";
        }
        int length = value.length();
        if (length <= VISIBLE_SUFFIX) {
            return String.valueOf(MASK_CHAR).repeat(length);
        }
        int maskedCount = length - VISIBLE_SUFFIX;
        return String.valueOf(MASK_CHAR).repeat(maskedCount) + value.substring(maskedCount);
    }
}
