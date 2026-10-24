package com.utility.docvalidation.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.utility.docvalidation.algorithm.VerhoeffAlgorithm;
import com.utility.docvalidation.dto.ValidationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end test exercising the real wiring (controller, service, registry,
 * validators, algorithms) over HTTP for all three document types.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Validation API (end to end)")
class ValidationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private VerhoeffAlgorithm verhoeff;

    private String url() {
        return "http://localhost:" + port + "/api/v1/validate";
    }

    private ResponseEntity<ValidationResponse> post(String type, String number) {
        String body = "{\"documentType\":\"" + type + "\",\"documentNumber\":\"" + number + "\"}";
        return rest.postForEntity(url(),
                new org.springframework.http.HttpEntity<>(body, jsonHeaders()),
                ValidationResponse.class);
    }

    private org.springframework.http.HttpHeaders jsonHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void shouldValidatePanEndToEnd() {
        ResponseEntity<ValidationResponse> response = post("PAN", "ABCPE1234F");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().documentNumber()).isEqualTo("ABCPE1234F");
        assertThat(response.getBody().isValid()).isTrue();
    }

    @Test
    void shouldValidateCardEndToEnd() {
        ResponseEntity<ValidationResponse> response = post("CARD", "4111 1111 1111 1111");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isTrue();
    }

    @Test
    void shouldValidateAadhaarEndToEnd() {
        String aadhaar = "99999999999" + verhoeff.generateCheckDigit("99999999999");
        ResponseEntity<ValidationResponse> response = post("AADHAAR", aadhaar);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isTrue();
    }

    @Test
    void shouldValidateVidAliasEndToEnd() {
        String vid = "999999999999999" + verhoeff.generateCheckDigit("999999999999999");
        ResponseEntity<ValidationResponse> response = post("vid", vid);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isTrue();
    }

    @Test
    void shouldReturn400_whenDocumentNumberEmpty() {
        String body = "{\"documentType\":\"PAN\",\"documentNumber\":\"\"}";
        ResponseEntity<String> response = rest.postForEntity(url(),
                new org.springframework.http.HttpEntity<>(body, jsonHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
