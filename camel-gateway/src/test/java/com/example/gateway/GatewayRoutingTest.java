package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Point the gateway route at a WireMock fake orchestrator and verify
 * proxying plus the retry filter behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    static final WireMockServer orchestrator = new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void startFakeOrchestrator() {
        orchestrator.start();
    }

    @AfterAll
    static void stopFakeOrchestrator() {
        orchestrator.stop();
    }

    @BeforeEach
    void resetStubState() {
        // stubs and request journals must not leak between tests
        orchestrator.resetAll();
    }

    @DynamicPropertySource
    static void pointRouteAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("orchestrator.base-url", orchestrator::baseUrl);
    }

    @LocalServerPort
    int port;

    @Test
    void proxiesRequestAndResponseToTheOrchestrator() {
        orchestrator.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":{\"orderId\":\"A-1\"}}")));

        given()
                .header("X-Correlation-Id", "corr-gw-1")
                .contentType("application/json")
                .body("{\"order\":\"A-1\"}")
        .when()
                .post("http://localhost:" + port + "/process")
        .then()
                .statusCode(200)
                .body("response.orderId", equalTo("A-1"));

        orchestrator.verify(1, postRequestedFor(urlEqualTo("/process")));
    }

    @Test
    void retriesConnectionFailuresTwiceThenFailsFast() {
        orchestrator.stubFor(post(urlEqualTo("/process"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        given()
                .contentType("application/json")
                .body("{\"order\":\"A-1\"}")
        .when()
                .post("http://localhost:" + port + "/process")
        .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // connection resets are the retryable case: 1 initial attempt + 2 retries
        orchestrator.verify(3, postRequestedFor(urlEqualTo("/process")));
    }

    @Test
    void doesNotRetryCompletedResponses() {
        orchestrator.stubFor(post(urlEqualTo("/process")).willReturn(serverError()));

        given()
                .contentType("application/json")
                .body("{\"order\":\"A-1\"}")
        .when()
                .post("http://localhost:" + port + "/process")
        .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // 5xx responses are never retried: the POST may already have done work
        orchestrator.verify(1, postRequestedFor(urlEqualTo("/process")));
    }
}
