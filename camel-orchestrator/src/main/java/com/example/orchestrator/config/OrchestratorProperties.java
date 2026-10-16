package com.example.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator")
public record OrchestratorProperties(
        String correlationHeader,
        DefaultError defaultError,
        Http http,
        Apis apis) {

    public record DefaultError(int status, String message) {
    }

    public record Http(int connectTimeout, int requestTimeout) {
    }

    public record Apis(Endpoint endpoint, Token token, Endpoint process, Endpoint audit) {
    }

    public record Endpoint(String name) {
    }

    public record Token(String url, String requestBody, String tokenField) {
    }
}
