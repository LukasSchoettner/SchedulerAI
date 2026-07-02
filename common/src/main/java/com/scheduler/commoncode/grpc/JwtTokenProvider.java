package com.scheduler.commoncode.grpc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("Internal JWT provider key (base64): {}", Base64.getEncoder().encodeToString(keyBytes));
    }

    private Key key;

    public String getToken() {
        return Jwts.builder()
                .claim("id", 1) // dummy
                .claim("type", "internal") // distinguish internal token
                .setSubject("internal-service")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
