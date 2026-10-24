package com.utility.docvalidation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DocumentType")
class DocumentTypeTest {

    @ParameterizedTest(name = "shouldParse [{0} -> {1}]")
    @CsvSource({
            "PAN,PAN", "pan,PAN", "  Pan  ,PAN",
            "CARD,CARD", "card,CARD",
            "AADHAAR,AADHAAR", "aadhaar,AADHAAR",
            "VID,AADHAAR", "vid,AADHAAR", "Vid,AADHAAR"
    })
    void shouldParseCaseInsensitivelyWithAliases(String raw, DocumentType expected) {
        assertThat(DocumentType.parse(raw)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "PASSPORT", "GSTIN", "xyz"})
    void shouldReturnEmpty_whenUnsupported(String raw) {
        assertThat(DocumentType.parse(raw)).isEqualTo(Optional.empty());
    }

    @Test
    void shouldListSupportedTokensIncludingAlias() {
        assertThat(DocumentType.supportedTokens()).isEqualTo("PAN, CARD, AADHAAR, VID");
    }
}
