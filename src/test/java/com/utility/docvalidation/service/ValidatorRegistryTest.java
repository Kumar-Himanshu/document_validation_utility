package com.utility.docvalidation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.validator.DocumentValidator;
import com.utility.docvalidation.validator.card.CardValidator;
import com.utility.docvalidation.validator.pan.PanValidator;
import com.utility.docvalidation.algorithm.LuhnAlgorithm;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidatorRegistry (Open/Closed)")
class ValidatorRegistryTest {

    /**
     * A dummy validator for a hypothetical new type, declared as AADHAAR here
     * only to reuse an existing enum constant. The point of the test is that a
     * new {@link DocumentValidator} is discovered purely by being in the bean
     * list, with no change to the registry, service or controller.
     */
    private static final class DummyValidator implements DocumentValidator {
        @Override
        public DocumentType type() {
            return DocumentType.AADHAAR;
        }

        @Override
        public boolean validate(String value) {
            return "DUMMY".equals(value);
        }
    }

    @Test
    void shouldPickUpNewValidator_withoutChangingExistingCode() {
        DocumentValidator dummy = new DummyValidator();
        ValidatorRegistry registry = new ValidatorRegistry(List.of(
                new PanValidator(),
                new CardValidator(new LuhnAlgorithm()),
                dummy));

        assertThat(registry.find(DocumentType.AADHAAR)).contains(dummy);
        assertThat(registry.find(DocumentType.AADHAAR))
                .get()
                .satisfies(v -> assertThat(v.validate("DUMMY")).isTrue());
        assertThat(registry.supportedTypes())
                .containsExactlyInAnyOrder(DocumentType.PAN, DocumentType.CARD, DocumentType.AADHAAR);
    }

    @Test
    void shouldReturnEmpty_whenTypeNotRegistered() {
        ValidatorRegistry registry = new ValidatorRegistry(List.of(new PanValidator()));
        assertThat(registry.find(DocumentType.CARD)).isEmpty();
    }

    @Test
    void shouldReject_whenTwoValidatorsClaimSameType() {
        assertThatThrownBy(() -> new ValidatorRegistry(List.of(new PanValidator(), new PanValidator())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate validator");
    }
}
