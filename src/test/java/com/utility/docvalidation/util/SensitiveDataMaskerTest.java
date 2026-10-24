package com.utility.docvalidation.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SensitiveDataMasker")
class SensitiveDataMaskerTest {

    @Test
    void shouldRevealOnlyLastFourCharacters() {
        assertThat(SensitiveDataMasker.mask("ABCPE1234F")).isEqualTo("******234F");
    }

    @Test
    void shouldFullyMaskShortValues() {
        assertThat(SensitiveDataMasker.mask("1234")).isEqualTo("****");
        assertThat(SensitiveDataMasker.mask("12")).isEqualTo("**");
    }

    @Test
    void shouldHandleNull() {
        assertThat(SensitiveDataMasker.mask(null)).isEqualTo("null");
    }
}
