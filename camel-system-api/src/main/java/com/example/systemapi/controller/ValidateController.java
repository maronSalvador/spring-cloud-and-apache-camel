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

import java.util.Map;

/**
 * Backs step 1 of the orchestration flow: payload validation.
 * Returns {"status":"VALID"} for a non-empty JSON payload, 400 otherwise.
 */
@Slf4j
@RestController
@RequestMapping("/validate")
public class ValidateController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> validate(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody(required = false) String body) {

        log.info("[validate] correlationId={} body={}", correlationId, body);

        if (body == null || body.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "INVALID", "message", "Request body is empty"));
        }
        return ResponseEntity.ok(Map.of("status", "VALID"));
    }
}
