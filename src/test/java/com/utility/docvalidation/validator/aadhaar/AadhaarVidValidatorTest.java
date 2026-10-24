package com.utility.docvalidation.validator.aadhaar;

import static org.assertj.core.api.Assertions.assertThat;

import com.utility.docvalidation.algorithm.VerhoeffAlgorithm;
import com.utility.docvalidation.dto.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AadhaarVidValidator")
class AadhaarVidValidatorTest {

    private final VerhoeffAlgorithm verhoeff = new VerhoeffAlgorithm();
    private final AadhaarVidValidator validator = new AadhaarVidValidator(verhoeff);

    /** Builds a valid number of the given total length starting from a chosen first digit. */
    private String validNumber(int totalLength, char firstDigit) {
        StringBuilder payload = new StringBuilder();
        payload.append(firstDigit);
        for (int i = 1; i < totalLength - 1; i++) {
            payload.append((char) ('0' + (i % 9) + 1)); // digits 1..9, avoids leading-zero issues
        }
        int check = verhoeff.generateCheckDigit(payload.toString());
        return payload + String.valueOf(check);
    }

    @Test
    void shouldReportAadhaarType() {
        assertThat(validator.type()).isEqualTo(DocumentType.AADHAAR);
    }

    @Test
    void shouldReturnTrue_whenValidTwelveDigitAadhaar() {
        assertThat(validator.validate(validNumber(12, '9'))).isTrue();
    }

    @Test
    void shouldReturnTrue_whenValidSixteenDigitVid() {
        assertThat(validator.validate(validNumber(16, '9'))).isTrue();
    }

    @Test
    void shouldReturnTrue_whenSeparatorsPresent() {
        String valid = validNumber(12, '9');
        String spaced = valid.substring(0, 4) + " " + valid.substring(4, 8) + " " + valid.substring(8);
        String hyphenated = valid.substring(0, 4) + "-" + valid.substring(4, 8) + "-" + valid.substring(8);
        assertThat(validator.validate(spaced)).isTrue();
        assertThat(validator.validate(hyphenated)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenSingleDigitChanged() {
        String valid = validNumber(12, '9');
        char[] chars = valid.toCharArray();
        chars[4] = (char) ('0' + ((chars[4] - '0' + 1) % 10));
        assertThat(validator.validate(new String(chars))).isFalse();
    }

    @Test
    void shouldReturnFalse_whenAdjacentDigitsTransposed() {
        String valid = validNumber(12, '9');
        char[] chars = valid.toCharArray();
        // Swap positions 1 and 2 (guaranteed to differ given the payload pattern).
        char tmp = chars[1];
        chars[1] = chars[2];
        chars[2] = tmp;
        String transposed = new String(chars);
        assertThat(transposed).isNotEqualTo(valid);
        assertThat(validator.validate(transposed)).isFalse();
    }

    @ParameterizedTest(name = "shouldReturnFalse_whenStartsWithZeroOrOne [first={0}]")
    @ValueSource(chars = {'0', '1'})
    void shouldReturnFalse_whenStartsWithZeroOrOne(char first) {
        // Build a Verhoeff-valid number, then it still must fail on the first-digit rule.
        assertThat(validator.validate(validNumber(12, first))).isFalse();
    }

    @ParameterizedTest(name = "shouldReturnFalse_whenLengthIsWrong [len={0}]")
    @ValueSource(ints = {11, 13, 15, 17})
    void shouldReturnFalse_whenLengthIsWrong(int length) {
        assertThat(validator.validate(validNumber(length, '9'))).isFalse();
    }

    @Test
    void shouldReturnFalse_whenContainsLetters() {
        assertThat(validator.validate("23456789012X")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldReturnFalse_whenNullEmptyOrBlank(String number) {
        assertThat(validator.validate(number)).isFalse();
    }
}
