package com.example.systemapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs step 3 of the orchestration flow: payload processing.
 * Consumes the client JSON payload and returns an XML document —
 * the orchestrator's XmlToJsonProcessor converts it back to JSON
 * for the client response.
 */
@Slf4j
@RestController
@RequestMapping("/process")
public class ProcessController {

    public record ProcessRequest(String order) {
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> process(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody(required = false) ProcessRequest request) {

        log.info("[process] correlationId={} request={}", correlationId, request);

        if (request == null || request.order() == null || request.order().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"order is required\"}");
        }
        String xml = "<response><orderId>%s</orderId><status>PROCESSED</status></response>"
                .formatted(request.order());
        return ResponseEntity.ok(xml);
    }
}
