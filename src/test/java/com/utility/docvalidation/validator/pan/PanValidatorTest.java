package com.utility.docvalidation.validator.pan;

import static org.assertj.core.api.Assertions.assertThat;

import com.utility.docvalidation.dto.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PanValidator")
class PanValidatorTest {

    private final PanValidator validator = new PanValidator();

    @Test
    void shouldReportPanType() {
        assertThat(validator.type()).isEqualTo(DocumentType.PAN);
    }

    @ParameterizedTest(name = "shouldReturnTrue_whenCategoryCharacterIsAllowed [{0}]")
    @ValueSource(strings = {
            "ABCPE1234F", "ABCCE1234F", "ABCHE1234F", "ABCFE1234F", "ABCAE1234F",
            "ABCTE1234F", "ABCBE1234F", "ABCGE1234F", "ABCLE1234F", "ABCJE1234F"
    })
    void shouldReturnTrue_whenCategoryCharacterIsAllowed(String pan) {
        assertThat(validator.validate(pan)).isTrue();
    }

    @ParameterizedTest(name = "shouldReturnFalse_whenPanHasInvalidCategoryCharacter [{0}]")
    @ValueSource(strings = {"ABCZE1234F", "ABCDE1234F", "ABCEE1234F", "ABCKE1234F", "ABCQE1234F"})
    void shouldReturnFalse_whenPanHasInvalidCategoryCharacter(String pan) {
        assertThat(validator.validate(pan)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenNumericBlockIsZeroes() {
        assertThat(validator.validate("ABCPE0000F")).isFalse();
    }

    @Test
    void shouldReturnTrue_whenNumericBlockIsMinimum() {
        assertThat(validator.validate("ABCPE0001F")).isTrue();
    }

    @Test
    void shouldReturnTrue_whenInputIsLowercase() {
        assertThat(validator.validate("abcpe1234f")).isTrue();
    }

    @Test
    void shouldReturnTrue_whenInputHasSurroundingWhitespace() {
        assertThat(validator.validate("  abcpe1234f  ")).isTrue();
    }

    @Test
    void shouldReturnFalse_whenLastCharacterIsNotLetter() {
        assertThat(validator.validate("ABCPE12345")).isFalse();
    }

    @ParameterizedTest(name = "shouldReturnFalse_whenLengthIsWrong [{0}]")
    @ValueSource(strings = {"ABCPE1234", "ABCPE1234FX", "ABCP1234F"})
    void shouldReturnFalse_whenLengthIsWrong(String pan) {
        assertThat(validator.validate(pan)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldReturnFalse_whenNullEmptyOrBlank(String pan) {
        assertThat(validator.validate(pan)).isFalse();
    }
}
