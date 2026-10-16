package com.example.orchestrator.processor;

import com.example.orchestrator.config.OrchestratorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The one error exit for the whole flow: a fixed, configured default
 * error response, enriched with the client's correlation id for tracing.
 */
public class DefaultErrorResponseProcessor implements Processor {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final OrchestratorProperties config;

    public DefaultErrorResponseProcessor(OrchestratorProperties config) {
        this.config = config;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ERROR");
        body.put("message", config.defaultError().message());
        body.put("correlationId", exchange.getProperty("correlationId", String.class));

        exchange.getIn().setBody(JSON.writeValueAsString(body));
        exchange.getIn().setHeader("Content-Type", "application/json");
        exchange.getIn().setHeader("CamelHttpResponseCode", config.defaultError().status());
    }
}
