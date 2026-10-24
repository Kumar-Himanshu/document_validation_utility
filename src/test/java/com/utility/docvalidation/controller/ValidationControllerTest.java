package com.utility.docvalidation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.utility.docvalidation.dto.DocumentType;
import com.utility.docvalidation.dto.ValidationRequest;
import com.utility.docvalidation.dto.ValidationResponse;
import com.utility.docvalidation.exception.UnsupportedDocumentTypeException;
import com.utility.docvalidation.service.DocumentValidationService;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ValidationController.class)
@DisplayName("ValidationController")
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentValidationService service;

    @Test
    void shouldReturn200WithIsValidTrue_whenValueIsValid() throws Exception {
        when(service.validate(any(ValidationRequest.class)))
                .thenReturn(new ValidationResponse("ABCPE1234F", true));

        mockMvc.perform(post("/api/v1/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"PAN\",\"documentNumber\":\"ABCPE1234F\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("ABCPE1234F"))
                .andExpect(jsonPath("$.isValid").value(true));
    }

    @Test
    void shouldReturn200WithIsValidFalse_whenValueFailsValidation() throws Exception {
        when(service.validate(any(ValidationRequest.class)))
                .thenReturn(new ValidationResponse("ABCZE1234F", false));

        mockMvc.perform(post("/api/v1/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"PAN\",\"documentNumber\":\"ABCZE1234F\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false));
    }

    @Test
    void shouldReturn400_whenDocumentNumberBlank() throws Exception {
        mockMvc.perform(post("/api/v1/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"PAN\",\"documentNumber\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/validate"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400WithSupportedTypes_whenTypeUnsupported() throws Exception {
        when(service.validate(any(ValidationRequest.class)))
                .thenThrow(new UnsupportedDocumentTypeException(
                        "Unsupported documentType. Supported types: PAN, CARD, AADHAAR, VID"));

        mockMvc.perform(post("/api/v1/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"PASSPORT\",\"documentNumber\":\"X123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Unsupported documentType. Supported types: PAN, CARD, AADHAAR, VID"));
    }

    @Test
    void shouldReturn400_whenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/validate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500Generic_whenUnexpectedFailure() throws Exception {
        when(service.validate(any(ValidationRequest.class)))
                .thenThrow(new RuntimeException("boom - should never leak"));

        mockMvc.perform(post("/api/v1/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"PAN\",\"documentNumber\":\"ABCPE1234F\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(
                        "An unexpected error occurred. Please try again later."));
    }

    @Test
    void shouldReturnSupportedTypes() throws Exception {
        when(service.supportedTypes())
                .thenReturn(Set.of(DocumentType.PAN, DocumentType.CARD, DocumentType.AADHAAR));

        mockMvc.perform(get("/api/v1/validate/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
