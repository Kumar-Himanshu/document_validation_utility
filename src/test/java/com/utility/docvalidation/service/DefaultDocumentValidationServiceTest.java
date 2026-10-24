package com.utility.docvalidation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.dto.ValidationRequest;
import com.utility.docvalidation.dto.ValidationResponse;
import com.utility.docvalidation.exception.UnsupportedDocumentTypeException;
import com.utility.docvalidation.validator.DocumentValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DefaultDocumentValidationService")
@ExtendWith(MockitoExtension.class)
class DefaultDocumentValidationServiceTest {

    @Mock
    private DocumentValidator panValidator;

    @Mock
    private DocumentValidator cardValidator;

    private DefaultDocumentValidationService service;

    @BeforeEach
    void setUp() {
        when(panValidator.type()).thenReturn(DocumentType.PAN);
        when(cardValidator.type()).thenReturn(DocumentType.CARD);
        ValidatorRegistry registry = new ValidatorRegistry(List.of(panValidator, cardValidator));
        service = new DefaultDocumentValidationService(registry, new SimpleMeterRegistry());
    }

    @Test
    void shouldRouteToPanValidator_andEchoDocumentNumber() {
        when(panValidator.validate("ABCPE1234F")).thenReturn(true);

        ValidationResponse response = service.validate(new ValidationRequest("pan", "ABCPE1234F"));

        assertThat(response.documentNumber()).isEqualTo("ABCPE1234F");
        assertThat(response.isValid()).isTrue();
        verify(cardValidator, never()).validate(anyString());
    }

    @Test
    void shouldRouteVidAliasToAadhaarRoute_butFailWhenNoAadhaarValidatorRegistered() {
        // Only PAN and CARD validators are registered here, so VID -> AADHAAR has no validator.
        assertThatThrownBy(() -> service.validate(new ValidationRequest("vid", "123")))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void shouldThrow_whenDocumentTypeUnsupported() {
        assertThatThrownBy(() -> service.validate(new ValidationRequest("PASSPORT", "X")))
                .isInstanceOf(UnsupportedDocumentTypeException.class)
                .hasMessageContaining("PAN, CARD, AADHAAR, VID");
    }

    @Test
    void shouldExposeSupportedTypesFromRegistry() {
        assertThat(service.supportedTypes()).containsExactlyInAnyOrder(DocumentType.PAN, DocumentType.CARD);
    }
}
