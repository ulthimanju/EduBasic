package com.app.auth.auth.service;

import com.app.auth.LogMessages;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Date;

/**
 * Stateless JWT utility — all six methods are atomic and side-effect-free.
 *
 * <p>Uses JJWT 0.12 API. Algorithm: RS256 (Asymmetric RSA).
 * Keys are loaded from env or generated on startup.</p>
 */
@Service
@Slf4j
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey  publicKey;

    @Value("${app.jwt.expiration-seconds}")
    private long expirationSeconds;

    @Value("${app.jwt.refresh-expiration-seconds:86400}")
    private long refreshExpirationSeconds;

    public JwtService(@Value("${app.jwt.private-key:}") String privateKeyStr,
                      @Value("${app.jwt.public-key:}")  String publicKeyStr) throws Exception {
        if (privateKeyStr.isEmpty() || publicKeyStr.isEmpty()) {
            log.info("Generating new RSA key pair for JWT signing (RS256)...");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey  = keyPair.getPublic();
        } else {
            log.info("Loading RSA keys from environment variables...");
            this.privateKey = loadPrivateKey(privateKeyStr);
            this.publicKey  = loadPublicKey(publicKeyStr);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Generate a signed Access Token (JWT).
     */
    public String generateToken(String userId, String email, String jwtId, java.util.Collection<String> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000L);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .id(jwtId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate a signed Refresh Token.
     */
    public String generateRefreshToken(String userId, String jwtId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationSeconds * 1000L);

        return Jwts.builder()
                .subject(userId)
                .id(jwtId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Returns the expiry as an {@link Instant} for a freshly-issued token.
     */
    public Instant getExpiryInstant() {
        return Instant.now().plusSeconds(expirationSeconds);
    }

    public Instant getRefreshExpiryInstant() {
        return Instant.now().plusSeconds(refreshExpirationSeconds);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug(LogMessages.JWT_VALIDATION_FAILED, e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    // ── Claim extraction ────────────────────────────────────────────────────

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJwtId(String token) {
        return parseClaims(token).getId();
    }

    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", java.util.List.class);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey loadPrivateKey(String key) throws Exception {
        byte[] encoded = java.util.Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey(String key) throws Exception {
        byte[] encoded = java.util.Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }
}
