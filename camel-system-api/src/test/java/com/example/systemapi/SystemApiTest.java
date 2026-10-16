package com.example.systemapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemApiTest {

    @Autowired
    TestRestTemplate rest;

    private HttpEntity<String> entity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", "test-corr");
        return new HttpEntity<>(body, headers);
    }

    @Test
    void validateAcceptsNonEmptyPayload() {
        var response = rest.exchange("/validate", HttpMethod.POST, entity("{\"order\":\"A-1\"}"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"VALID\"");
    }

    @Test
    void validateRejectsEmptyPayload() {
        var response = rest.exchange("/validate", HttpMethod.POST, entity("   "), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"status\":\"INVALID\"");
    }

    @Test
    void tokenIssuesAccessTokenForClientCredentialsGrant() {
        var response = rest.exchange("/token", HttpMethod.POST,
                entity("{\"grant_type\":\"client_credentials\",\"client_id\":\"demo\",\"client_secret\":\"secret\"}"),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"access_token\"");
        assertThat(response.getBody()).contains("\"token_type\":\"Bearer\"");
    }

    @Test
    void tokenRejectsUnsupportedGrant() {
        var response = rest.exchange("/token", HttpMethod.POST,
                entity("{\"grant_type\":\"password\"}"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void processReturnsXmlEchoingOrderId() {
        var response = rest.exchange("/process", HttpMethod.POST, entity("{\"order\":\"A-1\"}"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_XML)).isTrue();
        assertThat(response.getBody()).contains("<orderId>A-1</orderId>");
        assertThat(response.getBody()).contains("<status>PROCESSED</status>");
    }

    @Test
    void processRejectsMissingOrder() {
        var response = rest.exchange("/process", HttpMethod.POST, entity("{}"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
