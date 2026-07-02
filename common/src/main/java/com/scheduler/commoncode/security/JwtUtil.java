package com.scheduler.commoncode.security;

import io.jsonwebtoken.Claims;
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
import java.util.Locale;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration-ms}")
    private long expirationTimeMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(secretString.getBytes());
        log.info("JWT key (base64): {}", Base64.getEncoder().encodeToString(keyBytes));
    }

    public String generateToken(Long id, String customername, String email) {
        return generateToken(id, customername, email, null);
    }

    public String generateToken(Long id, String customername, String email, Enum<?> membershipLevel) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .setSubject(customername)
                .claim("email", email)
                .claim("id", id)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationTimeMs));

        if (membershipLevel != null) {
            builder.claim("membershipLevel", membershipLevel.name());
        }

        return builder
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long extractCustomerId(String token) {
        Object id = getClaims(token).get("id");
        if (id instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(id));
    }

    public String extractMembershipLevel(String token) {
        Object membershipLevel = getClaims(token).get("membershipLevel");
        return membershipLevel != null
                ? String.valueOf(membershipLevel).toUpperCase(Locale.ROOT)
                : null;
    }

    public String extractSubject(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }
}
