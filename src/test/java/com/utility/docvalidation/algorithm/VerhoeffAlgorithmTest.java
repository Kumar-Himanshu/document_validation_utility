package com.utility.docvalidation.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("VerhoeffAlgorithm")
class VerhoeffAlgorithmTest {

    private final VerhoeffAlgorithm verhoeff = new VerhoeffAlgorithm();

    @Test
    void shouldReturnTrue_whenKnownGoodVector() {
        // Well-known published Verhoeff example: 236 with check digit 3.
        assertThat(verhoeff.isValid("2363")).isTrue();
    }

    @ParameterizedTest(name = "shouldReturnTrue_whenGeneratedCheckDigit [payload={0}]")
    @ValueSource(strings = {
            "23456789012",       // 11-digit payload -> 12-digit Aadhaar
            "234567890123456",   // 15-digit payload -> 16-digit VID
            "987654321098"
    })
    void shouldReturnTrue_whenGeneratedCheckDigit(String payload) {
        String valid = VerhoeffTestSupport.withCheckDigit(payload);
        assertThat(verhoeff.isValid(valid)).isTrue();
    }

    @Test
    void shouldReturnFalse_whenSingleDigitIsChanged() {
        String valid = VerhoeffTestSupport.withCheckDigit("23456789012");
        String mutated = VerhoeffTestSupport.mutateSingleDigit(valid, 3);
        assertThat(verhoeff.isValid(mutated)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenAdjacentDigitsAreTransposed() {
        String valid = VerhoeffTestSupport.withCheckDigit("23456789012");
        String transposed = VerhoeffTestSupport.transposeAdjacent(valid, 2); // '4' and '5' differ
        assertThat(transposed).isNotEqualTo(valid);
        assertThat(verhoeff.isValid(transposed)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnFalse_whenNullOrEmpty(String value) {
        assertThat(verhoeff.isValid(value)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenContainsNonDigit() {
        assertThat(verhoeff.isValid("23x6")).isFalse();
    }

    @Test
    void shouldThrow_whenGeneratingCheckDigitForInvalidPayload() {
        assertThatThrownBy(() -> verhoeff.generateCheckDigit("12x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verhoeff.generateCheckDigit(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verhoeff.generateCheckDigit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
