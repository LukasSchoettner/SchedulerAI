package com.scheduler.apigateway.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.SecretKey;

@Configuration
public class JwtDecoderConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // build a SecretKey from your plain-text secret
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return NimbusReactiveJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}

