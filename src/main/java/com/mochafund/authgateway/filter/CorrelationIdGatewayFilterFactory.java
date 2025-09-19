package com.mochafund.authgateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CorrelationIdGatewayFilterFactory extends AbstractGatewayFilterFactory<CorrelationIdGatewayFilterFactory.Config> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public CorrelationIdGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String correlationId = UUID.randomUUID().toString();

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .build();

            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}