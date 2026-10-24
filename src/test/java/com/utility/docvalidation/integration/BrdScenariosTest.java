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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Covers the 15 indicative scenarios from BRD section 4, end to end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("BRD section 4 indicative scenarios")
class BrdScenariosTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private VerhoeffAlgorithm verhoeff;

    private ResponseEntity<ValidationResponse> validate(String type, String number) {
        String body = "{\"documentType\":\"" + type + "\",\"documentNumber\":\"" + number + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity("http://localhost:" + port + "/api/v1/validate",
                new HttpEntity<>(body, headers), ValidationResponse.class);
    }

    private boolean isValid(String type, String number) {
        ResponseEntity<ValidationResponse> response = validate(type, number);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().isValid();
    }

    @Test
    void scenario1_panValid() {
        assertThat(isValid("PAN", "ABCPE1234F")).isTrue();
    }

    @Test
    void scenario2_panInvalidCategoryLetter() {
        assertThat(isValid("PAN", "ABCZE1234F")).isFalse();
    }

    @Test
    void scenario3_panNumericBlockZeroes() {
        assertThat(isValid("PAN", "ABCPE0000F")).isFalse();
    }

    @Test
    void scenario4_panNormalisedToUpperCase() {
        assertThat(isValid("PAN", "abcpe1234f")).isTrue();
    }

    @Test
    void scenario5_panLastCharacterNotLetter() {
        assertThat(isValid("PAN", "ABCPE12345")).isFalse();
    }

    @Test
    void scenario6_cardValidLuhn() {
        assertThat(isValid("CARD", "4111 1111 1111 1111")).isTrue();
    }

    @Test
    void scenario7_cardFailsLuhn() {
        assertThat(isValid("CARD", "4111 1111 1111 1112")).isFalse();
    }

    @Test
    void scenario8_cardTooShort() {
        assertThat(isValid("CARD", "1234")).isFalse();
    }

    @Test
    void scenario9_aadhaarValidCheckDigit() {
        String aadhaar = "23456789012" + verhoeff.generateCheckDigit("23456789012");
        assertThat(isValid("AADHAAR", aadhaar)).isTrue();
    }

    @Test
    void scenario10_aadhaarOneDigitChanged() {
        String aadhaar = "23456789012" + verhoeff.generateCheckDigit("23456789012");
        char[] chars = aadhaar.toCharArray();
        chars[5] = (char) ('0' + ((chars[5] - '0' + 1) % 10));
        assertThat(isValid("AADHAAR", new String(chars))).isFalse();
    }

    @Test
    void scenario11_aadhaarAdjacentTransposition() {
        String aadhaar = "23456789012" + verhoeff.generateCheckDigit("23456789012");
        char[] chars = aadhaar.toCharArray();
        char tmp = chars[1];
        chars[1] = chars[2];
        chars[2] = tmp;
        assertThat(new String(chars)).isNotEqualTo(aadhaar);
        assertThat(isValid("AADHAAR", new String(chars))).isFalse();
    }

    @Test
    void scenario12_aadhaarStartsWithZeroOrOne() {
        String startsZero = "03456789012" + verhoeff.generateCheckDigit("03456789012");
        String startsOne = "13456789012" + verhoeff.generateCheckDigit("13456789012");
        assertThat(isValid("AADHAAR", startsZero)).isFalse();
        assertThat(isValid("AADHAAR", startsOne)).isFalse();
    }

    @Test
    void scenario13_vidValidCheckDigit() {
        String vid = "234567890123456" + verhoeff.generateCheckDigit("234567890123456");
        assertThat(isValid("VID", vid)).isTrue();
    }

    @Test
    void scenario14_emptyValueReturns400() {
        String body = "{\"documentType\":\"PAN\",\"documentNumber\":\"\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/validate",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void scenario15_unknownTypeReturns400() {
        String body = "{\"documentType\":\"PASSPORT\",\"documentNumber\":\"ABC123\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/validate",
                new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("PAN, CARD, AADHAAR, VID");
    }
}
