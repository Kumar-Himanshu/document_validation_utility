package com.utility.docvalidation.algorithm;

import org.springframework.stereotype.Component;

/**
 * Luhn (mod-10) checksum algorithm, used to catch accidental typing errors in
 * payment card numbers.
 *
 * <p>Walking from the rightmost digit leftwards, every second digit is doubled
 * (and reduced by nine when the product exceeds nine). The value is valid when
 * the sum of all resulting digits is a multiple of ten.
 *
 * <p>This component is stateless and therefore thread-safe.
 */
@Component
public class LuhnAlgorithm implements ChecksumAlgorithm {

    private static final int RADIX = 10;
    private static final int DOUBLE_OVERFLOW_ADJUSTMENT = 9;

    /**
     * {@inheritDoc}
     *
     * @param digits a non-null string containing digits only; any other input
     *               (null, empty, non-digit) is treated as invalid
     */
    @Override
    public boolean isValid(String digits) {
        if (digits == null || digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
            return false;
        }

        int sum = 0;
        boolean doubleThisDigit = false;
        // Traverse right to left so the rightmost (check) digit is never doubled.
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleThisDigit) {
                digit *= 2;
                if (digit > DOUBLE_OVERFLOW_ADJUSTMENT) {
                    digit -= DOUBLE_OVERFLOW_ADJUSTMENT;
                }
            }
            sum += digit;
            doubleThisDigit = !doubleThisDigit;
        }

        return sum % RADIX == 0;
    }
}
