package com.ai.assistant.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000; // 24h

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Long userId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("uid", Long.class);
    }
}
