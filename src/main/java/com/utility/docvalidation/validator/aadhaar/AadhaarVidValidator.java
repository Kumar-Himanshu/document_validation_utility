package com.utility.docvalidation.validator.aadhaar;

import com.utility.docvalidation.algorithm.VerhoeffAlgorithm;
import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.validator.DocumentValidator;
import org.springframework.stereotype.Component;

/**
 * Validates Aadhaar numbers and VIDs using the Verhoeff checksum (BRD section 3.3).
 *
 * <p>Rules:
 * <ul>
 *   <li>Strip spaces and hyphens, then the value must be digits only.</li>
 *   <li>Length must be 12 (Aadhaar) or 16 (VID); any other length is invalid.</li>
 *   <li>The first digit must not be 0 or 1.</li>
 *   <li>The value must pass the Verhoeff checksum.</li>
 * </ul>
 *
 * <p>Both Aadhaar and VID route here because they share the Verhoeff scheme and
 * differ only in length. The checksum is delegated to {@link VerhoeffAlgorithm}.
 * Stateless and thread-safe.
 */
@Component
public class AadhaarVidValidator implements DocumentValidator {

    private static final int AADHAAR_LENGTH = 12;
    private static final int VID_LENGTH = 16;
    private static final char FIRST_DIGIT_ZERO = '0';
    private static final char FIRST_DIGIT_ONE = '1';

    private final VerhoeffAlgorithm verhoeffAlgorithm;

    /**
     * @param verhoeffAlgorithm the Verhoeff checksum implementation (constructor injected)
     */
    public AadhaarVidValidator(VerhoeffAlgorithm verhoeffAlgorithm) {
        this.verhoeffAlgorithm = verhoeffAlgorithm;
    }

    @Override
    public DocumentType type() {
        return DocumentType.AADHAAR;
    }

    @Override
    public boolean validate(String value) {
        if (value == null) {
            return false;
        }
        String cleaned = value.replaceAll("[\\s-]", "");
        if (!cleaned.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int length = cleaned.length();
        if (length != AADHAAR_LENGTH && length != VID_LENGTH) {
            return false;
        }
        char first = cleaned.charAt(0);
        if (first == FIRST_DIGIT_ZERO || first == FIRST_DIGIT_ONE) {
            return false;
        }
        return verhoeffAlgorithm.isValid(cleaned);
    }
}
