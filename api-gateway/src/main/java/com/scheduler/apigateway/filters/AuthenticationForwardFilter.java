package com.scheduler.apigateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationForwardFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        // after the built‐in AUTHENTICATION filter
        return SecurityWebFiltersOrder.AUTHENTICATION.getOrder() + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(token -> {
                    Jwt jwt = token.getToken();
                    String userId = jwt.getClaimAsString("id");
                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

}

