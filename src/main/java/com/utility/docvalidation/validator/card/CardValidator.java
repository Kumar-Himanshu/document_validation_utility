package com.utility.docvalidation.validator.card;

import com.utility.docvalidation.algorithm.LuhnAlgorithm;
import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.validator.DocumentValidator;
import org.springframework.stereotype.Component;

/**
 * Validates credit/debit card numbers using the Luhn checksum (BRD section 3.2).
 *
 * <p>Rules:
 * <ul>
 *   <li>Strip spaces and hyphens, then the value must be digits only.</li>
 *   <li>Length must be between 13 and 19 digits inclusive.</li>
 *   <li>The cleaned value must pass the Luhn checksum.</li>
 * </ul>
 *
 * <p>The checksum itself is delegated to {@link LuhnAlgorithm} so the algorithm
 * stays reusable and independently testable (Single Responsibility).
 * Stateless and thread-safe.
 */
@Component
public class CardValidator implements DocumentValidator {

    private static final int MIN_LENGTH = 13;
    private static final int MAX_LENGTH = 19;

    private final LuhnAlgorithm luhnAlgorithm;

    /**
     * @param luhnAlgorithm the Luhn checksum implementation (constructor injected)
     */
    public CardValidator(LuhnAlgorithm luhnAlgorithm) {
        this.luhnAlgorithm = luhnAlgorithm;
    }

    @Override
    public DocumentType type() {
        return DocumentType.CARD;
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
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return false;
        }
        return luhnAlgorithm.isValid(cleaned);
    }
}
