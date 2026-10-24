package com.utility.docvalidation.algorithm;

/**
 * Test helper that builds known-good Verhoeff numbers of arbitrary length and
 * provides mutation utilities, so tests can assert that single-digit errors and
 * adjacent transpositions are caught.
 */
final class VerhoeffTestSupport {

    private static final VerhoeffAlgorithm ALGORITHM = new VerhoeffAlgorithm();

    private VerhoeffTestSupport() {
    }

    /**
     * Builds a valid Verhoeff number of the requested total length by appending
     * the correct check digit to the given payload.
     *
     * @param payload the leading digits (without check digit); length must be
     *                {@code totalLength - 1}
     * @return a value that passes {@link VerhoeffAlgorithm#isValid(String)}
     */
    static String withCheckDigit(String payload) {
        int check = ALGORITHM.generateCheckDigit(payload);
        return payload + check;
    }

    /**
     * Changes a single digit at the given index (wrapping 9 -> 0), producing a
     * value that should fail validation.
     */
    static String mutateSingleDigit(String value, int index) {
        char[] chars = value.toCharArray();
        int digit = chars[index] - '0';
        chars[index] = (char) ('0' + ((digit + 1) % 10));
        return new String(chars);
    }

    /**
     * Swaps two adjacent, differing digits starting at the given index,
     * producing a transposition that Verhoeff must catch.
     *
     * @return the transposed value, or the original if the two digits are equal
     */
    static String transposeAdjacent(String value, int index) {
        char[] chars = value.toCharArray();
        if (chars[index] == chars[index + 1]) {
            return value; // caller should pick an index where digits differ
        }
        char tmp = chars[index];
        chars[index] = chars[index + 1];
        chars[index + 1] = tmp;
        return new String(chars);
    }
}
