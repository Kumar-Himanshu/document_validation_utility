package com.utility.docvalidation.validator.pan;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.validator.DocumentValidator;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates Indian PAN (Permanent Account Number) values.
 *
 * <p>Rules (BRD section 3.1):
 * <ul>
 *   <li>Trim surrounding whitespace and upper-case before validating.</li>
 *   <li>Match {@code ^[A-Z]{3}[PCHFATBGLJ][A-Z][0-9]{4}[A-Z]$}.</li>
 *   <li>The four-digit numeric block must not be {@code 0000}.</li>
 *   <li>The 10th character is only checked for being a letter; the check-letter
 *       formula is not public and is deliberately not recomputed.</li>
 * </ul>
 *
 * <p>Stateless and thread-safe.
 */
@Component
public class PanValidator implements DocumentValidator {

    /** Full PAN structure: 3 letters, category letter, letter, 4 digits, letter. */
    private static final Pattern PAN_PATTERN =
            Pattern.compile("^[A-Z]{3}[PCHFATBGLJ][A-Z][0-9]{4}[A-Z]$");

    /** The numeric block occupies characters 6 to 9 (zero-based indices 5..8). */
    private static final int NUMERIC_BLOCK_START = 5;
    private static final int NUMERIC_BLOCK_END = 9;
    private static final String INVALID_NUMERIC_BLOCK = "0000";

    @Override
    public DocumentType type() {
        return DocumentType.PAN;
    }

    @Override
    public boolean validate(String value) {
        if (value == null) {
            return false;
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT);
        if (!PAN_PATTERN.matcher(normalised).matches()) {
            return false;
        }
        String numericBlock = normalised.substring(NUMERIC_BLOCK_START, NUMERIC_BLOCK_END);
        return !INVALID_NUMERIC_BLOCK.equals(numericBlock);
    }
}
