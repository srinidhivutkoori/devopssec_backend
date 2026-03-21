package com.whiteboard.app.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Component responsible for JWT token lifecycle management.
 * Handles generation of new tokens after successful authentication,
 * extraction of claims from existing tokens, and validation of token integrity.
 *
 * Uses HMAC-SHA256 (HS256) signing algorithm with a configurable secret key.
 * The secret must be at least 256 bits (32 ASCII characters) for HS256.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /**
     * The HMAC-SHA256 signing secret loaded from application properties.
     * Must be kept confidential and never committed to version control in production.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Token validity duration in milliseconds (default: 86400000 = 24 hours).
     */
    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Generate a new JWT token for the given username.
     * The token encodes the username as the subject claim and includes
     * standard issued-at and expiration timestamps.
     *
     * @param username the authenticated user's username to encode in the token
     * @return signed JWT token string
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)           // Subject claim: the username
                .issuedAt(now)               // iat: when the token was issued
                .expiration(expiration)      // exp: when the token expires
                .signWith(getSigningKey())   // Sign with HMAC-SHA256
                .compact();
    }

    /**
     * Extract the username (subject claim) from a valid JWT token.
     *
     * @param token the JWT token string
     * @return the username stored in the subject claim
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Get the token expiration duration for client-side display purposes.
     *
     * @return expiration duration in milliseconds
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Validate a JWT token by verifying its signature and checking expiration.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token has expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("JWT signature validation failed: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Parse and verify the JWT token, returning its claims payload.
     * Throws JwtException subtypes on any validation failure.
     *
     * @param token the JWT token to parse
     * @return the verified Claims object
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Derive the SecretKey instance from the Base64-encoded secret string.
     * The key is used for both signing new tokens and verifying incoming tokens.
     *
     * @return the HMAC-SHA256 signing key
     */
    private SecretKey getSigningKey() {
        // Encode the raw secret string to bytes using UTF-8, then wrap as HMAC key
        byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
