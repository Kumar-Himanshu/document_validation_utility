package com.utility.docvalidation.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LuhnAlgorithm")
class LuhnAlgorithmTest {

    private final LuhnAlgorithm luhn = new LuhnAlgorithm();

    @ParameterizedTest(name = "shouldReturnTrue_whenKnownGoodCardNumber [{0}]")
    @ValueSource(strings = {
            "4111111111111111",   // well-known Visa test number
            "4539148803436467",   // BRD worked example (sum 80)
            "5500005555555559",
            "6011000990139424",
            "4012888888881881"
    })
    void shouldReturnTrue_whenKnownGoodCardNumber(String number) {
        assertThat(luhn.isValid(number)).isTrue();
    }

    @ParameterizedTest(name = "shouldReturnFalse_whenSingleDigitError [{0}]")
    @ValueSource(strings = {
            "4111111111111112",   // last digit mutated
            "4539148803436468",
            "1234567812345678"
    })
    void shouldReturnFalse_whenSingleDigitError(String number) {
        assertThat(luhn.isValid(number)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnFalse_whenNullOrEmpty(String number) {
        assertThat(luhn.isValid(number)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenContainsNonDigit() {
        assertThat(luhn.isValid("4111a111111111b1")).isFalse();
    }
}
