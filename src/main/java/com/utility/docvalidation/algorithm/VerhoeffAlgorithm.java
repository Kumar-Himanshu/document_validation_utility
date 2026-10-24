package com.utility.docvalidation.algorithm;

import org.springframework.stereotype.Component;

/**
 * Verhoeff check-digit algorithm, used for Aadhaar and VID numbers.
 *
 * <p>The scheme is based on the dihedral group D5 and detects all single-digit
 * errors and all adjacent transposition errors. It relies on three published
 * lookup tables: a multiplication table {@code d}, a permutation table
 * {@code p} and an inverse table {@code inv}.
 *
 * <p>This component is stateless (the tables are immutable shared constants)
 * and therefore thread-safe. It also exposes a check-digit generator, used by
 * tests to produce known-good vectors.
 */
@Component
public class VerhoeffAlgorithm implements ChecksumAlgorithm {

    /** Multiplication table {@code d} (dihedral group D5). Non-commutative. */
    private static final int[][] D = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    /** Permutation table {@code p}; the row used depends on the digit position. */
    private static final int[][] P = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    /** Inverse table {@code inv}, used when generating a check digit. */
    private static final int[] INV = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

    private static final int PERMUTATION_ROWS = 8;

    /**
     * {@inheritDoc}
     *
     * <p>The supplied value must already include its trailing check digit.
     *
     * @param digits a non-null string containing digits only; any other input
     *               (null, empty, non-digit) is treated as invalid
     */
    @Override
    public boolean isValid(String digits) {
        if (digits == null || digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
            return false;
        }

        int c = 0;
        int length = digits.length();
        // Process right to left, starting at position i = 0 on the rightmost digit.
        for (int i = 0; i < length; i++) {
            int digit = digits.charAt(length - 1 - i) - '0';
            c = D[c][P[i % PERMUTATION_ROWS][digit]];
        }
        return c == 0;
    }

    /**
     * Generates the Verhoeff check digit for a payload that does not yet include
     * one. Appending the returned digit to {@code payload} yields a value that
     * passes {@link #isValid(String)}.
     *
     * <p>Provided to let tests build known-good vectors of arbitrary length.
     *
     * @param payload a non-null, non-empty string of digits without a check digit
     * @return the check digit (0-9)
     * @throws IllegalArgumentException if {@code payload} is null, empty or non-numeric
     */
    public int generateCheckDigit(String payload) {
        if (payload == null || payload.isEmpty() || !payload.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("payload must be a non-empty digit string");
        }

        int c = 0;
        int length = payload.length();
        // The check digit sits at position 0; the payload therefore starts at i = 1.
        for (int i = 0; i < length; i++) {
            int digit = payload.charAt(length - 1 - i) - '0';
            c = D[c][P[(i + 1) % PERMUTATION_ROWS][digit]];
        }
        return INV[c];
    }
}
