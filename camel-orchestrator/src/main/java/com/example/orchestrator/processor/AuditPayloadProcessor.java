package com.example.orchestrator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the audit record for the current step from the body that is about
 * to be logged (validate response, token response, or transformed JSON).
 */
public class AuditPayloadProcessor implements Processor {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("correlationId", exchange.getProperty("correlationId", String.class));
        audit.put("step", exchange.getProperty("step", String.class));
        audit.put("payload", exchange.getIn().getBody(String.class));
        audit.put("timestamp", Instant.now().toString());
        exchange.getIn().setBody(JSON.writeValueAsString(audit));
    }
}
