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
import java.util.UUID;

/**
 * Backs step 2 of the orchestration flow: client-credentials token issuance.
 * Accepts the configured grant body and returns an access_token the
 * orchestrator's TokenResponseProcessor picks up by field name.
 */
@Slf4j
@RestController
@RequestMapping("/token")
public class TokenController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody(required = false) Map<String, Object> grant) {

        log.info("[token] correlationId={} grant={}", correlationId, grant);

        if (grant == null || !"client_credentials".equals(grant.get("grant_type"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "unsupported_grant_type"));
        }
        return ResponseEntity.ok(Map.of(
                "access_token", UUID.randomUUID().toString(),
                "token_type", "Bearer",
                "expires_in", 3600));
    }
}
