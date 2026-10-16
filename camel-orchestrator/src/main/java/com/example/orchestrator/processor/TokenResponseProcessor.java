package com.example.orchestrator.processor;

import com.example.orchestrator.config.OrchestratorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Extracts the bearer token from the token API response.
 * Not cached — the token API is called on every client request.
 */
public class TokenResponseProcessor implements Processor {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final OrchestratorProperties config;

    public TokenResponseProcessor(OrchestratorProperties config) {
        this.config = config;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        JsonNode node = JSON.readTree(exchange.getIn().getBody(String.class));
        String token = node.path(config.apis().token().tokenField()).asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Token response did not contain field '" + config.apis().token().tokenField() + "'");
        }
        exchange.setProperty("authToken", "Bearer " + token);
    }
}
