package com.example.orchestrator.route;

import com.example.orchestrator.config.OrchestratorProperties;
import com.example.orchestrator.processor.CorrelationIdProcessor;
import com.example.orchestrator.processor.DefaultErrorResponseProcessor;
import com.example.orchestrator.processor.TokenResponseProcessor;
import com.example.orchestrator.processor.XmlToJsonProcessor;
import com.example.orchestrator.repository.dao.AuditLogRepository;
import com.example.orchestrator.repository.dao.SysEndpointsRepository;
import com.example.orchestrator.repository.entity.AuditLogEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrchestrationRoute extends RouteBuilder {

    private final OrchestratorProperties config;
    private final AuditLogRepository auditLogRepository;
    private final SysEndpointsRepository sysEndpointsRepository;

    private String validationUrl;
    private String tokenUrl;
    private String processUrl;

    // setup to fetch sys endpoints
    @PostConstruct
    private void init() {
        this.validationUrl = sysEndpointsRepository.findByName("validation_url").getEndpoint();
        this.tokenUrl = sysEndpointsRepository.findByName("token_url").getEndpoint();
        this.processUrl = sysEndpointsRepository.findByName("process_url").getEndpoint();
    }

    @Override
    public void configure() {

        // Fail-fast: covers downstream HTTP errors and timeouts and terminates the flow with a 500 response to the client
        onException(Exception.class)
                .handled(true)
                .log("Flow failed at step ${exchangeProperty.step} " +
                        "[correlationId=${exchangeProperty.correlationId}]: ${exception.message}")
                .process(new DefaultErrorResponseProcessor(config));

        // --- Main Orchestration Flow ---
        from("platform-http:/process?httpMethodRestrict=POST")
                .routeId("orchestrate-process")
                .process(new CorrelationIdProcessor(config))
                .setProperty("clientPayload", body())
                .to("direct:validate")
                .to("direct:fetchToken")                // flow order
                .to("direct:processPayload")
                .process(new XmlToJsonProcessor());

        // --- Step 1: Validate Payload --- represents atomic validator
        from("direct:validate")
                .routeId("step-validate")
                .removeHeaders("CamelHttp*")
                .setProperty("interfaceName", constant("VALIDATE_PAYLOAD"))
                .toD(httpCall(validationUrl, "POST"))
                .process(exchange -> {
                    exchange.setProperty("auditCorrelationId", exchange.getProperty("correlationId")); // setup for async db logging
                    exchange.setProperty("auditInterfaceName", exchange.getProperty("interfaceName"));
                    exchange.setProperty("auditBody", exchange.getIn().getBody(String.class));
                })
                .to("seda:db-audit?blockWhenFull=false"); // logs response to db same as ace entry/exit

        // --- Step 2: Fetch Bearer Token --- represents get bearer token for post finacle
        from("direct:fetchToken")
                .routeId("step-fetch-token")
                .removeHeaders("CamelHttp*")
                .setProperty("interfaceName", constant("FETCH_TOKEN"))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(simple("{{orchestrator.apis.token.request-body}}"))
                .toD(httpCall(tokenUrl, "POST"))
                .process(exchange -> {
                    exchange.setProperty("auditCorrelationId", exchange.getProperty("correlationId")); // setup for async db logging
                    exchange.setProperty("auditInterfaceName", exchange.getProperty("interfaceName"));
                    exchange.setProperty("auditBody", exchange.getIn().getBody(String.class));
                })
                .to("seda:db-audit?blockWhenFull=false") // logs response to db same as ace entry/exit
                .process(new TokenResponseProcessor(config));

        // --- Step 3: Process Payload --- represents post finacle
        from("direct:processPayload")
                .routeId("step-process-payload")
                .removeHeaders("CamelHttp*")
                .setProperty("interfaceName", constant("PROCESS_PAYLOAD"))
                .setHeader("Authorization", exchangeProperty("authToken"))
                .setHeader("Content-Type", constant("application/json"))
                .setBody(exchangeProperty("clientPayload"))
                .toD(httpCall(processUrl, "POST"))
                .process(exchange -> {
                    exchange.setProperty("auditCorrelationId", exchange.getProperty("correlationId"));
                    exchange.setProperty("auditInterfaceName", exchange.getProperty("interfaceName"));
                    exchange.setProperty("auditBody", exchange.getIn().getBody(String.class));
                })
                .to("seda:db-audit?blockWhenFull=false"); // logs response to db same as ace entry/exit

        // --- Background Async Database Worker ---
        // represents ace logging
        from("seda:db-audit?concurrentConsumers=4")
                .routeId("async-db-audit-worker")
                .process(exchange -> {
                    String correlationId = exchange.getProperty("auditCorrelationId", String.class);
                    String interfaceName = exchange.getProperty("auditStep", String.class);
                    String responseBody = exchange.getProperty("auditBody", String.class);

                    auditLogRepository.save(new AuditLogEntity(correlationId, interfaceName, responseBody));
                });
    }
    // utility for external api calls
    private String httpCall(String rawUrl, String httpMethod) {
        return rawUrl + "?httpMethod=" + httpMethod
                + "&bridgeEndpoint=true"
                + "&throwExceptionOnFailure=true"
                + "&connectTimeout={{orchestrator.http.connect-timeout}}"
                + "&socketTimeout={{orchestrator.http.request-timeout}}";
    }
}