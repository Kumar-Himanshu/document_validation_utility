package com.utility.docvalidation.dto;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The document types supported by the validation utility.
 *
 * <p>{@code VID} is not a distinct type: it is an alias that maps to the
 * {@link #AADHAAR} validator, because Aadhaar numbers and VIDs share the same
 * Verhoeff checksum scheme (they differ only in length). Parsing is
 * case-insensitive and alias-aware.
 */
public enum DocumentType {

    /** Permanent Account Number (India). */
    PAN,

    /** Credit or debit card number (Luhn). */
    CARD,

    /** Aadhaar number or VID (Verhoeff). */
    AADHAAR;

    /** Alias accepted on input and routed to {@link #AADHAAR}. */
    private static final String VID_ALIAS = "VID";

    /**
     * Parses a raw, possibly aliased, case-insensitive document type.
     *
     * @param raw the input value (may be null or blank)
     * @return the matching type, or {@link Optional#empty()} if unsupported
     */
    public static Optional<DocumentType> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        if (VID_ALIAS.equals(normalised)) {
            return Optional.of(AADHAAR);
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equals(normalised))
                .findFirst();
    }

    /**
     * Returns the human-readable list of accepted input tokens, including the
     * {@code VID} alias, for use in error messages and the types endpoint.
     *
     * @return a comma-separated list such as {@code "PAN, CARD, AADHAAR, VID"}
     */
    public static String supportedTokens() {
        String canonical = Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        return canonical + ", " + VID_ALIAS;
    }
}
