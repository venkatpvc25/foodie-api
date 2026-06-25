package com.pvc.foodie.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    public String generateAccessToken(String username) {
        Date expiresAt = new Date(System.currentTimeMillis() + accessExpiration);
        log.debug("Generating access token: username={}, expiresAt={}", username, expiresAt);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiresAt)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            extractUsername(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT rejected: reason={}", e.getMessage());
            return false;
        }
    }
}
