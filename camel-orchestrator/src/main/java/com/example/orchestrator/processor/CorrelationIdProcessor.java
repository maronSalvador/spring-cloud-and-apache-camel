package com.example.orchestrator.processor;

import com.example.orchestrator.config.OrchestratorProperties;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.UUID;

/**
 * Reads the client-supplied correlation id header into an exchange property
 * and keeps it as a message header so it propagates to every downstream call.
 * Generates one as a fallback if the header is missing.
 */
public class CorrelationIdProcessor implements Processor {

    private final OrchestratorProperties config;

    public CorrelationIdProcessor(OrchestratorProperties config) {
        this.config = config;
    }

    @Override
    public void process(Exchange exchange) {
        String header = config.correlationHeader();
        String correlationId = exchange.getIn().getHeader(header, String.class);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            exchange.getIn().setHeader(header, correlationId);
        }
        exchange.setProperty("correlationId", correlationId);
    }
}
