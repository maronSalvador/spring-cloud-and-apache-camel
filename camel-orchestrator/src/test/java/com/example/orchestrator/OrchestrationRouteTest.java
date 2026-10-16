package com.example.orchestrator;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrchestrationRouteTest {

    static final WireMockServer wireMock = new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void pointApisAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("orchestrator.apis.validate.url", () -> wireMock.baseUrl() + "/validate");
        registry.add("orchestrator.apis.token.url", () -> wireMock.baseUrl() + "/token");
        registry.add("orchestrator.apis.process.url", () -> wireMock.baseUrl() + "/process");
        registry.add("orchestrator.apis.audit.url", () -> wireMock.baseUrl() + "/audit");
    }

    @LocalServerPort
    int port;

    @Test
    void happyPathReturnsTransformedJsonAndAuditsEveryStep() {
        wireMock.stubFor(post(urlEqualTo("/validate"))
                .willReturn(aResponse().withBody("{\"status\":\"VALID\"}")));
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse().withBody("{\"access_token\":\"tok-123\"}")));
        wireMock.stubFor(post(urlEqualTo("/process"))
                .willReturn(ok().withBody("<response><orderId>A-1</orderId></response>")));
        wireMock.stubFor(post(urlEqualTo("/audit")).willReturn(ok()));

        given()
                .header("X-Correlation-Id", "corr-1")
                .contentType(ContentType.JSON)
                .body("{\"order\":\"A-1\"}")
        .when()
                .post("http://localhost:" + port + "/process")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("response.orderId", equalTo("A-1"));

        // audit called once after validate, once after token, once for the transformed JSON
        wireMock.verify(3, postRequestedFor(urlEqualTo("/audit")));
    }

    @Test
    void downstreamFailureTerminatesWithDefaultError() {
        wireMock.stubFor(post(urlEqualTo("/validate"))
                .willReturn(aResponse().withBody("{\"status\":\"VALID\"}")));
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse().withBody("{\"access_token\":\"tok-123\"}")));
        wireMock.stubFor(post(urlEqualTo("/process")).willReturn(serverError()));

        given()
                .header("X-Correlation-Id", "corr-2")
                .contentType(ContentType.JSON)
                .body("{\"order\":\"A-1\"}")
        .when()
                .post("http://localhost:" + port + "/process")
        .then()
                .statusCode(502)
                .contentType(ContentType.JSON)
                .body("status", equalTo("ERROR"))
                .body("correlationId", equalTo("corr-2"));
    }
}
