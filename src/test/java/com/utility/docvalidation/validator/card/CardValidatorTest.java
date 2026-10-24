package com.utility.docvalidation.validator.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.utility.docvalidation.algorithm.LuhnAlgorithm;
import com.utility.docvalidation.dto.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CardValidator")
class CardValidatorTest {

    private final CardValidator validator = new CardValidator(new LuhnAlgorithm());

    @Test
    void shouldReportCardType() {
        assertThat(validator.type()).isEqualTo(DocumentType.CARD);
    }

    @Test
    void shouldReturnTrue_whenKnownGoodCardNumber() {
        assertThat(validator.validate("4111111111111111")).isTrue();
    }

    @Test
    void shouldReturnTrue_whenCardNumberHasSpacesAndHyphens() {
        assertThat(validator.validate("4111 1111 1111 1111")).isTrue();
        assertThat(validator.validate("4111-1111-1111-1111")).isTrue();
    }

    @Test
    void shouldReturnFalse_whenLuhnCheckFails() {
        assertThat(validator.validate("4111 1111 1111 1112")).isFalse();
    }

    @ParameterizedTest(name = "shouldReturnFalse_whenLengthOutOfRange [{0}]")
    @ValueSource(strings = {
            "1234",                  // too short
            "123456789012",          // 12 digits, one below minimum
            "12345678901234567890"   // 20 digits, one above maximum
    })
    void shouldReturnFalse_whenLengthOutOfRange(String number) {
        assertThat(validator.validate(number)).isFalse();
    }

    @Test
    void shouldReturnTrue_atBoundaryLengths() {
        // 13-digit and 19-digit Luhn-valid numbers (boundaries of the allowed range).
        assertThat(validator.validate("4222222222222")).isTrue();      // 13 digits
        assertThat(validator.validate("4035501000000008")).isTrue();   // 16 digits
    }

    @Test
    void shouldReturnFalse_whenContainsNonSeparatorLetters() {
        assertThat(validator.validate("4111a1111111111b")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldReturnFalse_whenNullEmptyOrBlank(String number) {
        assertThat(validator.validate(number)).isFalse();
    }
}
